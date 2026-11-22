/**
 * @file billing.controller.ts
 * @module modules/billing
 * @description Controllers for:
 * 1. Idempotent monthly rent charge roll generation across all active tenants.
 * 2. Meter reading and unit rate updates (which recalculates bill & signed balances).
 * 3. Monthly billing status summary for the landlord.
 */

import { Request, Response } from 'express';
import { z } from 'zod';
import { db } from '../../db';
import { tenants, bills, ledgerEntries, rooms } from '../../db/schema';
import { eq, and, desc, inArray } from 'drizzle-orm';
import { calculateBillAmounts } from './ledger.util';

const batchMeterUpdateSchema = z.object({
  billingMonth: z.string().regex(/^\d{4}-\d{2}$/, 'Billing month must be in YYYY-MM format (e.g. 2026-10)').optional(),
  updates: z.array(
    z.object({
      tenantId: z.string().uuid('Valid tenant UUID required'),
      currentMeterReading: z.number().nonnegative('Reading must be >= 0'),
      electricityRatePerUnit: z.number().positive('Electricity rate must be positive').optional(),
    })
  ).min(1, 'At least one tenant reading update is required'),
});

const generateRentRollSchema = z.object({
  billingMonth: z.string().regex(/^\d{4}-\d{2}$/, 'Billing month must be in YYYY-MM format (e.g. 2026-10)').optional(),
});

/**
 * Returns current date's YYYY-MM if no month provided.
 */
function getCurrentBillingMonth(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/**
 * Idempotently generates monthly rent roll for all active tenants.
 * If a tenant already has a bill for this `billingMonth`, they are skipped.
 */
export async function generateMonthlyRentRollHandler(req: Request, res: Response): Promise<void> {
  const landlordId = (req as any).user?.userId;
  const parsed = generateRentRollSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const targetMonth = parsed.data.billingMonth || getCurrentBillingMonth();

  // Find all active tenants for this landlord
  const activeTenants = await db.query.tenants.findMany({
    where: landlordId
      ? and(eq(tenants.landlordId, landlordId), eq(tenants.isActive, true))
      : eq(tenants.isActive, true),
    with: {
      room: true,
      bills: {
        where: eq(bills.billingMonth, targetMonth),
      },
    },
  });

  const generatedList: Array<{ tenantId: string; tenantName: string; rent: number; status: string }> = [];
  const skippedList: Array<{ tenantId: string; tenantName: string; reason: string }> = [];

  for (const tenant of activeTenants) {
    // Check if bill already exists for targetMonth
    if (tenant.bills && tenant.bills.length > 0) {
      skippedList.push({
        tenantId: tenant.id,
        tenantName: tenant.name,
        reason: `Already billed for ${targetMonth}`,
      });
      continue;
    }

    const rentAmount = Number(tenant.allottedPrice);
    const prevMeterReading = Number(tenant.lastMeterReading);
    const electricityRate = Number(tenant.electricityRatePerUnit);

    // Initial bill on rent roll: rent charged, 0 electricity units (until readings entered)
    const [createdBill] = await db
      .insert(bills)
      .values({
        tenantId: tenant.id,
        billingMonth: targetMonth,
        rentAmount: rentAmount.toFixed(2),
        prevMeterReading: prevMeterReading.toFixed(2),
        currMeterReading: prevMeterReading.toFixed(2),
        unitsConsumed: '0.00',
        electricityRatePerUnit: electricityRate.toFixed(2),
        electricityAmount: '0.00',
        totalDueAmount: rentAmount.toFixed(2),
        status: 'UNPAID',
      })
      .returning();

    // Deduct rent from tenant's currentBalance
    const previousBalance = Number(tenant.currentBalance);
    const newBalance = Number((previousBalance - rentAmount).toFixed(2));

    await db
      .update(tenants)
      .set({
        currentBalance: newBalance.toFixed(2),
        updatedAt: new Date(),
      })
      .where(eq(tenants.id, tenant.id));

    // Log to ledger
    await db.insert(ledgerEntries).values({
      tenantId: tenant.id,
      type: 'BILL_CHARGE',
      amount: rentAmount.toFixed(2),
      runningBalance: newBalance.toFixed(2),
      description: `Monthly rent charge for ${targetMonth}`,
      referenceId: createdBill.id,
    });

    generatedList.push({
      tenantId: tenant.id,
      tenantName: tenant.name,
      rent: rentAmount,
      status: 'CHARGED',
    });
  }

  res.status(200).json({
    message: `Rent roll processed for ${targetMonth}`,
    billingMonth: targetMonth,
    generatedCount: generatedList.length,
    skippedCount: skippedList.length,
    generated: generatedList,
    skipped: skippedList,
  });
}

/**
 * Batch update electricity meter readings and rates for tenants.
 * Recalculates bills and updates tenant current balance and last meter reading.
 */
export async function batchUpdateMeterReadingsHandler(req: Request, res: Response): Promise<void> {
  const landlordId = (req as any).user?.userId;
  const parsed = batchMeterUpdateSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const { updates, billingMonth } = parsed.data;
  const targetMonth = billingMonth || getCurrentBillingMonth();

  const results: Array<any> = [];

  for (const item of updates) {
    const tenant = await db.query.tenants.findFirst({
      where: landlordId
        ? and(eq(tenants.id, item.tenantId), eq(tenants.landlordId, landlordId))
        : eq(tenants.id, item.tenantId),
      with: {
        room: true,
        bills: {
          where: eq(bills.billingMonth, targetMonth),
          limit: 1,
        },
      },
    });

    if (!tenant) {
      continue;
    }

    const rate = item.electricityRatePerUnit !== undefined ? item.electricityRatePerUnit : Number(tenant.electricityRatePerUnit);
    const prevReading = Number(tenant.lastMeterReading);
    const currReading = item.currentMeterReading;

    if (currReading < prevReading) {
      res.status(400).json({
        error: `Current meter reading (${currReading}) cannot be less than previous reading (${prevReading}) for tenant ${tenant.name}`,
      });
      return;
    }

    const unitsConsumed = Number((currReading - prevReading).toFixed(2));
    const electricityAmount = Number((unitsConsumed * rate).toFixed(2));

    // Update or create bill for this month
    let bill = tenant.bills?.[0];
    let electricityDelta = electricityAmount;

    if (bill) {
      const oldElectricityAmount = Number(bill.electricityAmount);
      electricityDelta = Number((electricityAmount - oldElectricityAmount).toFixed(2));

      const newTotalDue = Number((Number(bill.rentAmount) + electricityAmount).toFixed(2));

      await db
        .update(bills)
        .set({
          currMeterReading: currReading.toFixed(2),
          unitsConsumed: unitsConsumed.toFixed(2),
          electricityRatePerUnit: rate.toFixed(2),
          electricityAmount: electricityAmount.toFixed(2),
          totalDueAmount: newTotalDue.toFixed(2),
        })
        .where(eq(bills.id, bill.id));
    } else {
      // If no bill exists yet, create one
      const rentAmount = Number(tenant.allottedPrice);
      const totalDue = Number((rentAmount + electricityAmount).toFixed(2));

      const [newBill] = await db
        .insert(bills)
        .values({
          tenantId: tenant.id,
          billingMonth: targetMonth,
          rentAmount: rentAmount.toFixed(2),
          prevMeterReading: prevReading.toFixed(2),
          currMeterReading: currReading.toFixed(2),
          unitsConsumed: unitsConsumed.toFixed(2),
          electricityRatePerUnit: rate.toFixed(2),
          electricityAmount: electricityAmount.toFixed(2),
          totalDueAmount: totalDue.toFixed(2),
          status: 'UNPAID',
        })
        .returning();
      bill = newBill;
      // In this case both rent and electricity are new charges
      electricityDelta = totalDue;
    }

    // Deduct delta from currentBalance
    const previousBalance = Number(tenant.currentBalance);
    const newBalance = Number((previousBalance - electricityDelta).toFixed(2));

    await db
      .update(tenants)
      .set({
        currentBalance: newBalance.toFixed(2),
        lastMeterReading: currReading.toFixed(2),
        electricityRatePerUnit: rate.toFixed(2),
        updatedAt: new Date(),
      })
      .where(eq(tenants.id, tenant.id));

    // If electricity was charged, log in ledger
    if (electricityDelta > 0) {
      await db.insert(ledgerEntries).values({
        tenantId: tenant.id,
        type: 'BILL_CHARGE',
        amount: electricityDelta.toFixed(2),
        runningBalance: newBalance.toFixed(2),
        description: `Electricity reading update (${unitsConsumed} units @ ₹${rate}/unit) for ${targetMonth}`,
        referenceId: bill?.id,
      });
    }

    results.push({
      tenantId: tenant.id,
      tenantName: tenant.name,
      roomNo: tenant.room?.roomNo || 'N/A',
      prevReading,
      currReading,
      unitsConsumed,
      electricityRate: rate,
      electricityAmount,
      newBalance,
    });
  }

  res.status(200).json({
    message: 'Meter readings and electricity bills updated successfully',
    billingMonth: targetMonth,
    updatedCount: results.length,
    results,
  });
}

/**
 * Returns billing summary and room meter status for the current/selected month.
 */
export async function getMonthlyBillingSummaryHandler(req: Request, res: Response): Promise<void> {
  const landlordId = (req as any).user?.userId;
  const month = (req.query.month as string) || getCurrentBillingMonth();

  const allTenants = await db.query.tenants.findMany({
    where: landlordId
      ? and(eq(tenants.landlordId, landlordId), eq(tenants.isActive, true))
      : eq(tenants.isActive, true),
    with: {
      room: true,
      bills: {
        where: eq(bills.billingMonth, month),
        limit: 1,
      },
    },
    orderBy: [desc(tenants.createdAt)],
  });

  const tenantSummaries = allTenants.map((t) => {
    const currentBill = t.bills?.[0];
    const prevReading = Number(t.lastMeterReading);
    const currentReading = currentBill ? Number(currentBill.currMeterReading) : prevReading;
    const unitsConsumed = currentBill ? Number(currentBill.unitsConsumed) : 0;
    const electricityAmount = currentBill ? Number(currentBill.electricityAmount) : 0;
    const rentAmount = currentBill ? Number(currentBill.rentAmount) : Number(t.allottedPrice);
    const totalDue = currentBill ? Number(currentBill.totalDueAmount) : rentAmount;

    return {
      tenantId: t.id,
      tenantName: t.name,
      phone: t.phone,
      roomNo: t.room?.roomNo || 'Unassigned',
      roomId: t.roomId,
      allottedRent: Number(t.allottedPrice),
      electricityRatePerUnit: Number(t.electricityRatePerUnit),
      lastMeterReading: prevReading,
      currentMeterReading: currentReading,
      unitsConsumed,
      electricityAmount,
      totalBillAmount: totalDue,
      currentBalance: Number(t.currentBalance),
      isBilledForMonth: !!currentBill,
    };
  });

  res.status(200).json({
    billingMonth: month,
    tenants: tenantSummaries,
  });
}
