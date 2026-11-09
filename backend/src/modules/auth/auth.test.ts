/**
 * @file auth.test.ts
 * @description TDD test suite for password hashing, JWT generation, and token verification.
 */

import { describe, it, expect } from 'vitest';
import { hashPassword, verifyPassword, generateTokens, verifyAccessToken, verifyRefreshToken } from './auth.service';

describe('Auth Service Utility & Tokens', () => {
  const password = 'SecretPassword123!';

  it('should hash a password and correctly verify it', async () => {
    const hash = await hashPassword(password);
    expect(hash).toBeDefined();
    expect(hash).not.toEqual(password);

    const isMatch = await verifyPassword(password, hash);
    expect(isMatch).toBe(true);

    const isWrong = await verifyPassword('WrongPassword', hash);
    expect(isWrong).toBe(false);
  });

  it('should generate valid access and refresh tokens', () => {
    const payload = { userId: 'user-uuid-1234', email: 'landlord@example.com' };
    const tokens = generateTokens(payload);

    expect(tokens.accessToken).toBeDefined();
    expect(tokens.refreshToken).toBeDefined();

    const decodedAccess = verifyAccessToken(tokens.accessToken);
    expect(decodedAccess.userId).toBe(payload.userId);
    expect(decodedAccess.email).toBe(payload.email);

    const decodedRefresh = verifyRefreshToken(tokens.refreshToken);
    expect(decodedRefresh.userId).toBe(payload.userId);
  });
});
