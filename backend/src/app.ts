/**
 * @file app.ts
 * @module app
 * @description Express server entry point.
 * Provides health check, graceful shutdown, and route registration.
 */

import express, { Request, Response } from 'express';
import cors from 'cors';
import { env } from './config/env';

export const app = express();

app.use(cors({ origin: env.ALLOWED_ORIGINS }));
app.use(express.json());

/**
 * Health check endpoint for Render cold-start polling.
 * When Render spins down after 15 mins, client hits this to detect server wakeup.
 */
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'ok',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
  });
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(env.PORT, () => {
    console.log(`🚀 10nts backend server running on port ${env.PORT} [${env.NODE_ENV}]`);
  });
}
