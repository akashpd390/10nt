/**
 * @file export.test.ts
 * @description TDD test suite for CSV and PDF generation utilities.
 */

import { describe, it, expect } from 'vitest';
import { generateCsvContent, generateInvoicePdfBuffer } from './export.util';

describe('Export Utilities (CSV & PDF)', () => {
  it('should format ledger entries and tenant balances into CSV format', async () => {
    const mockData = [
      {
        tenantName: 'Rahul Sharma',
        roomNo: '101',
        type: 'PAYMENT_RECEIVED',
        amount: '5000.00',
        runningBalance: '0.00',
        date: '2026-11-09',
        description: 'Monthly rent paid via UPI',
      },
      {
        tenantName: 'Amit Verma',
        roomNo: '102',
        type: 'BILL_CHARGE',
        amount: '5800.00',
        runningBalance: '-5800.00',
        date: '2026-11-01',
        description: 'Rent + 100 units electricity',
      },
    ];

    const csvString = await generateCsvContent(mockData);
    expect(csvString).toBeDefined();
    expect(csvString).toContain('Rahul Sharma');
    expect(csvString).toContain('101');
    expect(csvString).toContain('5000.00');
    expect(csvString).toContain('Amit Verma');
  });

  it('should generate a valid PDF buffer for a tenant invoice', async () => {
    const pdfBuffer = await generateInvoicePdfBuffer({
      tenantName: 'Rahul Sharma',
      roomNo: '101',
      billingMonth: '2026-11',
      rentAmount: 5000,
      prevReading: 120,
      currReading: 180,
      unitsConsumed: 60,
      electricityRate: 8,
      electricityAmount: 480,
      totalDueAmount: 5480,
      previousBalance: 0,
      finalBalance: -5480,
      generatedDate: '2026-11-09',
    });

    expect(pdfBuffer).toBeInstanceOf(Buffer);
    expect(pdfBuffer.length).toBeGreaterThan(500); // Standard minimal PDF is > 500 bytes
    // PDF Magic bytes check (%PDF-)
    expect(pdfBuffer.toString('utf-8', 0, 5)).toBe('%PDF-');
  });
});
