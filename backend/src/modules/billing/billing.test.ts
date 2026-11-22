/**
 * @file billing.test.ts
 * @module modules/billing
 * @description TDD unit tests for monthly billing calculations,
 * idempotent rent roll charges, and meter reading updates.
 */

import { describe, it, expect } from 'vitest';
import { calculateBillAmounts } from './ledger.util';

describe('Billing Module Calculations', () => {
  it('correctly calculates units consumed and electricity total', () => {
    const result = calculateBillAmounts({
      rentAmount: 6000,
      prevReading: 120,
      currReading: 155,
      electricityRate: 8.5,
    });

    expect(result.unitsConsumed).toBe(35);
    expect(result.electricityAmount).toBe(297.5);
    expect(result.totalDueAmount).toBe(6297.5);
  });

  it('rejects lower current meter reading than previous reading', () => {
    expect(() => {
      calculateBillAmounts({
        rentAmount: 5000,
        prevReading: 200,
        currReading: 190,
        electricityRate: 8.0,
      });
    }).toThrow('Current meter reading cannot be lower than previous reading');
  });

  it('handles zero unit consumption correctly', () => {
    const result = calculateBillAmounts({
      rentAmount: 7500,
      prevReading: 310,
      currReading: 310,
      electricityRate: 9.0,
    });

    expect(result.unitsConsumed).toBe(0);
    expect(result.electricityAmount).toBe(0);
    expect(result.totalDueAmount).toBe(7500);
  });
});
