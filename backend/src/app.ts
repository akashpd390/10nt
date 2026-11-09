/**
 * @file app.ts
 * @module app
 * @description Express server entry point.
 * Registers routes, middleware, and cold-start health checks.
 */

import express, { Request, Response } from 'express';
import cors from 'cors';
import { env } from './config/env';
import { authRouter } from './modules/auth/auth.routes';
import { roomsRouter } from './modules/rooms/rooms.routes';
import { tenantsRouter } from './modules/tenants/tenants.routes';
import { paymentsRouter } from './modules/payments/payments.routes';

export const app = express();

app.use(cors({ origin: env.ALLOWED_ORIGINS }));
app.use(express.json());

/**
 * Health check endpoint for Render cold-start polling.
 */
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'ok',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
  });
});

// Feature Routes
app.use('/api/auth', authRouter);
app.use('/api/rooms', roomsRouter);
app.use('/api/tenants', tenantsRouter);
app.use('/api/payments', paymentsRouter);

if (process.env.NODE_ENV !== 'test') {
  app.listen(env.PORT, () => {
    console.log(`🚀 10nts backend server running on port ${env.PORT} [${env.NODE_ENV}]`);
  });
}
