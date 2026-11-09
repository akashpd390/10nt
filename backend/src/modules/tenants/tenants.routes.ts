/**
 * @file tenants.routes.ts
 * @module modules/tenants
 * @description Routes for tenant operations.
 */

import { Router } from 'express';
import {
  getTenantsHandler,
  getTenantByIdHandler,
  createTenantHandler,
  updateTenantHandler,
  deleteTenantHandler,
} from './tenants.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const tenantsRouter = Router();

tenantsRouter.use(requireAuth);

tenantsRouter.get('/', getTenantsHandler);
tenantsRouter.get('/:id', getTenantByIdHandler);
tenantsRouter.post('/', createTenantHandler);
tenantsRouter.put('/:id', updateTenantHandler);
tenantsRouter.delete('/:id', deleteTenantHandler);
