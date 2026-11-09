/**
 * @file auth.service.ts
 * @module modules/auth
 * @description Provides password hashing, comparison, and JWT access/refresh token generation.
 */

import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { env } from '../../config/env';

export interface TokenPayload {
  userId: string;
  email: string;
}

export interface GeneratedTokens {
  accessToken: string;
  refreshToken: string;
}

/**
 * Hashes a plain-text password using bcrypt.
 * @param password Raw password.
 * @returns Hashed string.
 */
export async function hashPassword(password: string): Promise<string> {
  const saltRounds = 10;
  return bcrypt.hash(password, saltRounds);
}

/**
 * Verifies a plain-text password against a stored bcrypt hash.
 * @param password Raw password attempt.
 * @param hash Stored hash.
 * @returns True if password matches, false otherwise.
 */
export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

/**
 * Issues short-lived access token and 7-day refresh token.
 * @param payload User ID and email payload.
 * @returns Access and refresh token pair.
 */
export function generateTokens(payload: TokenPayload): GeneratedTokens {
  const accessToken = jwt.sign(payload, env.JWT_ACCESS_SECRET, {
    expiresIn: env.JWT_ACCESS_EXPIRY as any,
  });

  const refreshToken = jwt.sign({ userId: payload.userId }, env.JWT_REFRESH_SECRET, {
    expiresIn: env.JWT_REFRESH_EXPIRY as any,
  });

  return { accessToken, refreshToken };
}

/**
 * Verifies and decodes an access token.
 * @param token Bearer JWT access token.
 * @returns Decoded TokenPayload.
 */
export function verifyAccessToken(token: string): TokenPayload {
  return jwt.verify(token, env.JWT_ACCESS_SECRET) as TokenPayload;
}

/**
 * Verifies and decodes a refresh token.
 * @param token Refresh JWT.
 * @returns Decoded payload containing userId.
 */
export function verifyRefreshToken(token: string): { userId: string } {
  return jwt.verify(token, env.JWT_REFRESH_SECRET) as { userId: string };
}
