/**
 * @file index.ts
 * @module db
 * @description Neon Serverless PostgreSQL connection and Drizzle ORM client initialization.
 */

import { neon, neonConfig } from '@neondatabase/serverless';
import { drizzle } from 'drizzle-orm/neon-http';
import * as schema from './schema';
import { env } from '../config/env';

// For local testing or when DATABASE_URL is not configured yet, fallback cleanly
const sql = neon(env.DATABASE_URL || 'postgres://user:pass@localhost:5432/db');
export const db = drizzle(sql, { schema });
export type Database = typeof db;
