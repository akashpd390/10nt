/**
 * @file billing.routes.ts
 * @module modules/billing
 * @description Routes for idempotent monthly rent roll generation,
 * batch electricity meter readings update, and month-cycle inquiries.
 */

import { Router } from 'express';
import { requireAuth } from '../../middleware/auth.middleware';
import {
  generateMonthlyRentRollHandler,
  batchUpdateMeterReadingsHandler,
  getMonthlyBillingSummaryHandler,
} from './billing.controller';

export const billingRouter = Router();

billingRouter.use(requireAuth);

billingRouter.get('/summary', getMonthlyBillingSummaryHandler);
billingRouter.post('/generate-rent-roll', generateMonthlyRentRollHandler);
billingRouter.post('/update-readings', batchUpdateMeterReadingsHandler);
