/**
 * @file export.controller.ts
 * @module modules/export
 * @description Controllers to download CSV reports and stream PDF invoices.
 */

import { Request, Response } from 'express';
import { db } from '../../db';
import { tenants, rooms, ledgerEntries, bills } from '../../db/schema';
import { eq, desc } from 'drizzle-orm';
import { generateCsvContent, generateInvoicePdfBuffer } from './export.util';

/**
 * Exports current rent-roll / tenant balances as CSV.
 */
export async function exportTenantsCsvHandler(req: Request, res: Response): Promise<void> {
  const landlordId = (req as any).user?.userId;
  const allTenants = await db.query.tenants.findMany({
    where: landlordId ? eq(tenants.landlordId, landlordId) : undefined,
    with: {
      room: true,
    },
    orderBy: [desc(tenants.createdAt)],
  });

  const records = allTenants.map((t) => ({
    TenantName: t.name,
    Phone: t.phone,
    RoomNo: t.room?.roomNo || 'N/A',
    MonthlyRent: t.allottedPrice,
    ElectricityRate: t.electricityRatePerUnit,
    LastMeterReading: t.lastMeterReading,
    CurrentBalance: t.currentBalance,
    Status: Number(t.currentBalance) < 0 ? 'DUE' : Number(t.currentBalance) > 0 ? 'ADVANCE' : 'SETTLED',
    JoinedDate: t.joinedDate ? new Date(t.joinedDate).toISOString().split('T')[0] : '',
  }));

  const csvContent = await generateCsvContent(records);

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="tenants-report.csv"');
  res.status(200).send(csvContent);
}

/**
 * Exports comprehensive monthly rent & electricity bill per tenant in clean tabular format.
 * Suitable for printing and tear-off tenant dispatch.
 */
export async function exportMonthlyRentBillCsvHandler(req: Request, res: Response): Promise<void> {
  const landlordId = (req as any).user?.userId;
  const now = new Date();
  const defaultMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const targetMonth = (req.query.month as string) || defaultMonth;

  const allTenants = await db.query.tenants.findMany({
    where: landlordId ? eq(tenants.landlordId, landlordId) : undefined,
    with: {
      room: true,
      bills: {
        orderBy: [desc(bills.createdAt)],
      },
    },
    orderBy: [desc(tenants.createdAt)],
  });

  const records = allTenants.map((t) => {
    // Find bill matching month, or latest bill
    const bill = t.bills.find((b) => b.billingMonth === targetMonth) || t.bills[0];
    const prevReading = bill ? Number(bill.prevMeterReading) : Number(t.lastMeterReading);
    const currReading = bill ? Number(bill.currMeterReading) : Number(t.lastMeterReading);
    const units = bill ? Number(bill.unitsConsumed) : 0;
    const rate = bill ? Number(bill.electricityRatePerUnit) : Number(t.electricityRatePerUnit);
    const elecAmount = bill ? Number(bill.electricityAmount) : 0;
    const rentAmount = bill ? Number(bill.rentAmount) : Number(t.allottedPrice);
    const totalMonthBill = Number((rentAmount + elecAmount).toFixed(2));
    const bal = Number(t.currentBalance);
    const totalPayableDue = bal < 0 ? Math.abs(bal) : 0;
    const advanceCredit = bal > 0 ? bal : 0;

    return {
      'Billing Month': targetMonth,
      'Tenant Name': t.name,
      'Room No': t.room?.roomNo || 'N/A',
      'Mobile Number': t.phone,
      'Base Rent (₹)': rentAmount.toFixed(2),
      'Prev Meter Reading': prevReading.toFixed(2),
      'Curr Meter Reading': currReading.toFixed(2),
      'Units Consumed': units.toFixed(2),
      'Unit Rate (₹)': rate.toFixed(2),
      'Electricity Bill (₹)': elecAmount.toFixed(2),
      'This Month Bill (₹)': totalMonthBill.toFixed(2),
      'Total Pending Due (₹)': totalPayableDue.toFixed(2),
      'Advance Credit (₹)': advanceCredit.toFixed(2),
      'Net Balance Status': bal < 0 ? 'DUE' : bal > 0 ? 'ADVANCE' : 'SETTLED',
      'Bill Generated Date': bill ? new Date(bill.generatedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
    };
  });

  const csvContent = await generateCsvContent(records);

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', `attachment; filename="rent-bill-${targetMonth}.csv"`);
  res.status(200).send(csvContent);
}

/**
 * Exports full ledger history as CSV.
 */
export async function exportLedgerCsvHandler(_req: Request, res: Response): Promise<void> {
  const entries = await db.query.ledgerEntries.findMany({
    with: {
      tenant: true,
    },
    orderBy: [desc(ledgerEntries.createdAt)],
  });

  const records = entries.map((e) => ({
    TenantName: e.tenant?.name || 'Unknown',
    Type: e.type,
    Amount: e.amount,
    RunningBalance: e.runningBalance,
    Description: e.description,
    Date: new Date(e.createdAt).toISOString(),
  }));

  const csvContent = await generateCsvContent(records);

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="ledger-transactions.csv"');
  res.status(200).send(csvContent);
}

/**
 * Generates and streams printable PDF statement for a specific tenant.
 */
export async function exportTenantInvoicePdfHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;

  const tenant = await db.query.tenants.findFirst({
    where: eq(tenants.id, id),
    with: {
      room: true,
    },
  });

  if (!tenant) {
    res.status(404).json({ error: 'Tenant not found' });
    return;
  }

  const currentDate = new Date();
  const currentMonthStr = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`;

  const pdfBuffer = await generateInvoicePdfBuffer({
    tenantName: tenant.name,
    roomNo: tenant.room?.roomNo || 'Unassigned',
    billingMonth: currentMonthStr,
    rentAmount: Number(tenant.allottedPrice),
    prevReading: Number(tenant.lastMeterReading),
    currReading: Number(tenant.lastMeterReading), // Snapshot reading
    unitsConsumed: 0,
    electricityRate: Number(tenant.electricityRatePerUnit),
    electricityAmount: 0,
    totalDueAmount: Number(tenant.allottedPrice),
    previousBalance: Number(tenant.currentBalance),
    finalBalance: Number(tenant.currentBalance),
    generatedDate: currentDate.toLocaleDateString('en-IN'),
  });

  res.setHeader('Content-Type', 'application/pdf');
  res.setHeader('Content-Disposition', `inline; filename="invoice-${tenant.name.replace(/\s+/g, '_')}.pdf"`);
  res.status(200).send(pdfBuffer);
}
