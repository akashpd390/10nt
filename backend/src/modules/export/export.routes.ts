/**
 * @file export.routes.ts
 * @module modules/export
 * @description Routes for PDF invoice streaming and CSV report downloads.
 */

import { Router } from 'express';
import {
  exportTenantsCsvHandler,
  exportLedgerCsvHandler,
  exportTenantInvoicePdfHandler,
  exportMonthlyRentBillCsvHandler,
} from './export.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const exportRouter = Router();

exportRouter.use(requireAuth);

exportRouter.get('/tenants/csv', exportTenantsCsvHandler);
exportRouter.get('/bills/csv', exportMonthlyRentBillCsvHandler);
exportRouter.get('/ledger/csv', exportLedgerCsvHandler);
exportRouter.get('/tenants/:id/pdf', exportTenantInvoicePdfHandler);
