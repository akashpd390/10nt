/**
 * @file payments.controller.ts
 * @module modules/payments
 * @description Coordinates manual tenant payments, applies credit towards pending bills,
 * calculates new signed running balance, and writes immutable records to the ledger.
 */

import { Request, Response } from 'express';
import { z } from 'zod';
import { db } from '../../db';
import { payments, tenants, ledgerEntries, bills } from '../../db/schema';
import { eq, desc } from 'drizzle-orm';
import { applyPaymentToBalance, calculateBillAmounts } from '../billing/ledger.util';

const manualPaymentSchema = z.object({
  tenantId: z.string().uuid('Valid tenant UUID required'),
  amountPaid: z.number().positive('Amount paid must be greater than zero'),
  paymentMode: z.enum(['CASH', 'UPI', 'BANK_TRANSFER']).default('CASH'),
  paymentDate: z.string().optional(), // ISO date string, defaults to now
  notes: z.string().optional(),
  // Optional electricity reading update during payment
  currentElectricityReading: z.number().optional(),
});

/**
 * Returns recent payments.
 */
export async function getPaymentsHandler(_req: Request, res: Response): Promise<void> {
  const result = await db.query.payments.findMany({
    with: {
      tenant: true,
    },
    orderBy: [desc(payments.paymentDate)],
    limit: 50,
  });

  res.status(200).json({ payments: result });
}

/**
 * Records a manual payment entry:
 * 1. Reads current signed balance.
 * 2. Calculates new signed balance.
 * 3. Records payment receipt.
 * 4. Logs ledger entry.
 * 5. Returns SMS confirmation text template for mobile SIM dispatch.
 */
export async function createPaymentHandler(req: Request, res: Response): Promise<void> {
  const parsed = manualPaymentSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const { tenantId, amountPaid, paymentMode, paymentDate, notes, currentElectricityReading } = parsed.data;

  // Retrieve tenant
  const [tenant] = await db.select().from(tenants).where(eq(tenants.id, tenantId)).limit(1);
  if (!tenant) {
    res.status(404).json({ error: 'Tenant not found' });
    return;
  }

  const prevBalanceNum = Number(tenant.currentBalance);

  // If current electricity reading is provided, calculate and apply bill first
  let electricityDelta = 0;
  if (currentElectricityReading !== undefined && currentElectricityReading > Number(tenant.lastMeterReading)) {
    const billCalc = calculateBillAmounts({
      rentAmount: 0, // Rent billed separately or included in advance
      prevReading: Number(tenant.lastMeterReading),
      currReading: currentElectricityReading,
      electricityRate: Number(tenant.electricityRatePerUnit),
    });
    electricityDelta = billCalc.electricityAmount;
  }

  const balanceBeforePayment = prevBalanceNum - electricityDelta;
  const { newBalance, status } = applyPaymentToBalance(balanceBeforePayment, amountPaid);

  const txDate = paymentDate ? new Date(paymentDate) : new Date();

  // Insert payment record
  const [createdPayment] = await db
    .insert(payments)
    .values({
      tenantId,
      amountPaid: amountPaid.toFixed(2),
      paymentMode,
      paymentDate: txDate,
      notes: notes || `Manual payment via ${paymentMode}`,
      balanceBefore: balanceBeforePayment.toFixed(2),
      balanceAfter: newBalance.toFixed(2),
    })
    .returning();

  // Log ledger entry
  await db.insert(ledgerEntries).values({
    tenantId,
    type: 'PAYMENT_RECEIVED',
    amount: amountPaid.toFixed(2),
    runningBalance: newBalance.toFixed(2),
    description: `Payment of ₹${amountPaid} received via ${paymentMode}`,
    referenceId: createdPayment.id,
  });

  // Update tenant balance & meter reading
  await db
    .update(tenants)
    .set({
      currentBalance: newBalance.toFixed(2),
      lastMeterReading: currentElectricityReading
        ? currentElectricityReading.toFixed(2)
        : tenant.lastMeterReading,
      updatedAt: new Date(),
    })
    .where(eq(tenants.id, tenantId));

  // Generate standardized SMS notification template for Android mobile SIM dispatch
  const balanceNote =
    newBalance < 0
      ? `Remaining Due: Rs ${Math.abs(newBalance).toFixed(2)}`
      : newBalance > 0
      ? `Advance Balance: Rs ${newBalance.toFixed(2)}`
      : `All dues cleared (Settled)`;

  const smsText = `Dear ${tenant.name}, payment of Rs ${amountPaid.toFixed(2)} received on ${txDate.toLocaleDateString('en-IN')} via ${paymentMode}. ${balanceNote}. Thank you!`;

  res.status(201).json({
    message: 'Payment recorded successfully',
    payment: createdPayment,
    summary: {
      tenantName: tenant.name,
      tenantPhone: tenant.phone,
      amountPaid,
      paymentMode,
      previousBalance: prevBalanceNum,
      newBalance,
      status,
      smsTemplate: smsText,
    },
  });
}
