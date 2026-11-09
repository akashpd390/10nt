/**
 * @file auth.routes.ts
 * @module modules/auth
 * @description Authentication routing declarations.
 */

import { Router } from 'express';
import { loginHandler, refreshHandler, changePasswordHandler } from './auth.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const authRouter = Router();

authRouter.post('/login', loginHandler);
authRouter.post('/refresh', refreshHandler);
authRouter.post('/change-password', requireAuth, changePasswordHandler);
