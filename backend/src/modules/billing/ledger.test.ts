/**
 * @file ledger.test.ts
 * @description TDD test suite for financial ledger calculations, signed balance, and bills.
 */

import { describe, it, expect } from 'vitest';
import { calculateBillAmounts, applyPaymentToBalance } from './ledger.util';

describe('Ledger & Balance Math Utilities', () => {
  it('should accurately calculate electricity units and total due bill amount', () => {
    // Prev reading: 100, Curr reading: 150 -> 50 units. Rate: 8.00 -> 400 electricity. Rent: 5000. Total bill = 5400.
    const bill = calculateBillAmounts({
      rentAmount: 5000,
      prevReading: 100,
      currReading: 150,
      electricityRate: 8,
    });

    expect(bill.unitsConsumed).toBe(50);
    expect(bill.electricityAmount).toBe(400);
    expect(bill.totalDueAmount).toBe(5400);
  });

  it('should throw an error if current meter reading is less than previous', () => {
    expect(() =>
      calculateBillAmounts({
        rentAmount: 5000,
        prevReading: 200,
        currReading: 150,
        electricityRate: 8,
      })
    ).toThrowError('Current meter reading cannot be lower than previous reading');
  });

  it('should adjust signed balance correctly when payment is received (Partial, Exact, Advance)', () => {
    // 1. Starting with -5400 (arrears). Tenant pays 3000. New balance = -2400 (still owes 2400)
    const partialPayment = applyPaymentToBalance(-5400, 3000);
    expect(partialPayment.newBalance).toBe(-2400);
    expect(partialPayment.status).toBe('PARTIAL');

    // 2. Starting with -2400. Tenant pays 2400. New balance = 0 (settled)
    const exactPayment = applyPaymentToBalance(-2400, 2400);
    expect(exactPayment.newBalance).toBe(0);
    expect(exactPayment.status).toBe('SETTLED');

    // 3. Starting with 0. Tenant pays 7000 (advance for next month). New balance = +7000 (credit)
    const advancePayment = applyPaymentToBalance(0, 7000);
    expect(advancePayment.newBalance).toBe(7000);
    expect(advancePayment.status).toBe('ADVANCE');

    // 4. Starting with -1000. Tenant pays 3000. New balance = +2000 (cleared arrears and has 2000 advance)
    const clearedWithAdvance = applyPaymentToBalance(-1000, 3000);
    expect(clearedWithAdvance.newBalance).toBe(2000);
    expect(clearedWithAdvance.status).toBe('ADVANCE');
  });
});
