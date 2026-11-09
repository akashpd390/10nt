/**
 * @file ledger.util.ts
 * @module modules/billing
 * @description Pure mathematical utility for billing and signed balance calculations.
 * Ensures decimal precision and avoids floating point inaccuracies.
 */

export interface BillCalculationInput {
  rentAmount: number;
  prevReading: number;
  currReading: number;
  electricityRate: number;
}

export interface BillCalculationResult {
  unitsConsumed: number;
  electricityAmount: number;
  totalDueAmount: number;
}

export interface PaymentBalanceResult {
  newBalance: number;
  status: 'PARTIAL' | 'SETTLED' | 'ADVANCE';
}

/**
 * Calculates bill units and total amount from meter readings and rent.
 * @param input Rent, readings, and rate.
 * @returns Breakdown of units, electricity charge, and total bill.
 */
export function calculateBillAmounts(input: BillCalculationInput): BillCalculationResult {
  if (input.currReading < input.prevReading) {
    throw new Error('Current meter reading cannot be lower than previous reading');
  }

  const unitsConsumed = Number((input.currReading - input.prevReading).toFixed(2));
  const electricityAmount = Number((unitsConsumed * input.electricityRate).toFixed(2));
  const totalDueAmount = Number((input.rentAmount + electricityAmount).toFixed(2));

  return {
    unitsConsumed,
    electricityAmount,
    totalDueAmount,
  };
}

/**
 * Applies a manual payment to a signed running balance.
 * Balance semantics:
 * < 0: Overdue / Arrears
 * 0: Settled
 * > 0: Advance credit
 *
 * @param currentBalance Starting signed balance before payment.
 * @param amountPaid Money received from tenant.
 * @returns Updated signed balance and classification status.
 */
export function applyPaymentToBalance(currentBalance: number, amountPaid: number): PaymentBalanceResult {
  const newBalance = Number((currentBalance + amountPaid).toFixed(2));
  let status: 'PARTIAL' | 'SETTLED' | 'ADVANCE' = 'SETTLED';

  if (newBalance < 0) {
    status = 'PARTIAL';
  } else if (newBalance > 0) {
    status = 'ADVANCE';
  }

  return {
    newBalance,
    status,
  };
}
