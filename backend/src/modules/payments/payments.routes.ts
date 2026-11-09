/**
 * @file payments.routes.ts
 * @module modules/payments
 * @description Routes for payments and manual transaction entries.
 */

import { Router } from 'express';
import { getPaymentsHandler, createPaymentHandler } from './payments.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const paymentsRouter = Router();

paymentsRouter.use(requireAuth);

paymentsRouter.get('/', getPaymentsHandler);
paymentsRouter.post('/', createPaymentHandler);
