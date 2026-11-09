/**
 * @file dashboard.routes.ts
 * @module modules/dashboard
 * @description Routes for landlord dashboard metrics.
 */

import { Router } from 'express';
import { getDashboardOverviewHandler } from './dashboard.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const dashboardRouter = Router();

dashboardRouter.use(requireAuth);
dashboardRouter.get('/overview', getDashboardOverviewHandler);
