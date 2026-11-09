/**
 * @file env.ts
 * @module config
 * @description Centralized, schema-validated environment configuration.
 * Validates all required database, JWT, and Cloudinary settings on application startup.
 */

import * as dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  PORT: z.string().default('5000').transform((val) => parseInt(val, 10)),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  DATABASE_URL: z.string().default('postgres://neondb_owner:dummy@localhost:5432/neondb'),
  JWT_ACCESS_SECRET: z.string().min(4, 'JWT_ACCESS_SECRET must be at least 4 characters').default('super_secret_access_token_min_16_chars_fallback'),
  JWT_REFRESH_SECRET: z.string().min(4, 'JWT_REFRESH_SECRET must be at least 4 characters').default('super_secret_refresh_token_min_16_chars_fallback'),
  JWT_ACCESS_EXPIRY: z.string().default('15m'),
  JWT_REFRESH_EXPIRY: z.string().default('7d'),
  ADMIN_EMAIL: z.string().email().default('landlord@example.com'),
  ADMIN_DEFAULT_PASSWORD: z.string().min(8).default('ChangeMeOnFirstLogin123!'),
  CLOUDINARY_CLOUD_NAME: z.string().optional(),
  CLOUDINARY_API_KEY: z.string().optional(),
  CLOUDINARY_API_SECRET: z.string().optional(),
  ALLOWED_ORIGINS: z.string().default('*'),
});

export type EnvConfig = z.infer<typeof envSchema>;

export function validateEnv(): EnvConfig {
  const result = envSchema.safeParse(process.env);
  if (!result.success) {
    console.error('❌ Invalid environment variables:', result.error.format());
    throw new Error('Environment validation failed');
  }
  return result.data;
}

export const env = validateEnv();
