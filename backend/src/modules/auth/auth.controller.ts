/**
 * @file auth.controller.ts
 * @module modules/auth
 * @description Controller for authentication endpoints: login, token refresh, and password update.
 */

import { Request, Response } from 'express';
import { z } from 'zod';
import { db } from '../../db';
import { users } from '../../db/schema';
import { eq } from 'drizzle-orm';
import { hashPassword, verifyPassword, generateTokens, verifyRefreshToken } from './auth.service';
import { AuthenticatedRequest } from '../../middleware/auth.middleware';
import { env } from '../../config/env';

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

const changePasswordSchema = z.object({
  oldPassword: z.string().min(1),
  newPassword: z.string().min(8),
});

const refreshSchema = z.object({
  refreshToken: z.string().min(1),
});

/**
 * Handles landlord login.
 * Seeds initial admin account if users table is empty.
 */
export async function loginHandler(req: Request, res: Response): Promise<void> {
  const parsed = loginSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const { email, password } = parsed.data;

  // Retrieve user or seed default admin if first time
  let [user] = await db.select().from(users).where(eq(users.email, email)).limit(1);

  if (!user && email === env.ADMIN_EMAIL) {
    const defaultHash = await hashPassword(env.ADMIN_DEFAULT_PASSWORD);
    const [created] = await db
      .insert(users)
      .values({
        email: env.ADMIN_EMAIL,
        passwordHash: defaultHash,
      })
      .returning();
    user = created;
  }

  if (!user) {
    res.status(401).json({ error: 'Invalid credentials' });
    return;
  }

  const isPasswordValid = await verifyPassword(password, user.passwordHash);
  if (!isPasswordValid) {
    res.status(401).json({ error: 'Invalid credentials' });
    return;
  }

  const tokens = generateTokens({ userId: user.id, email: user.email });
  const refreshHash = await hashPassword(tokens.refreshToken);

  await db
    .update(users)
    .set({ refreshTokenHash: refreshHash, updatedAt: new Date() })
    .where(eq(users.id, user.id));

  res.status(200).json({
    message: 'Login successful',
    user: { id: user.id, email: user.email },
    tokens,
  });
}

/**
 * Rotates tokens using valid refresh token.
 */
export async function refreshHandler(req: Request, res: Response): Promise<void> {
  const parsed = refreshSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Refresh token is required' });
    return;
  }

  const { refreshToken } = parsed.data;

  try {
    const payload = verifyRefreshToken(refreshToken);
    const [user] = await db.select().from(users).where(eq(users.id, payload.userId)).limit(1);

    if (!user || !user.refreshTokenHash) {
      res.status(401).json({ error: 'Invalid session' });
      return;
    }

    const isValid = await verifyPassword(refreshToken, user.refreshTokenHash);
    if (!isValid) {
      res.status(401).json({ error: 'Invalid refresh token' });
      return;
    }

    const newTokens = generateTokens({ userId: user.id, email: user.email });
    const newRefreshHash = await hashPassword(newTokens.refreshToken);

    await db
      .update(users)
      .set({ refreshTokenHash: newRefreshHash, updatedAt: new Date() })
      .where(eq(users.id, user.id));

    res.status(200).json({ tokens: newTokens });
  } catch (error) {
    res.status(401).json({ error: 'Expired or invalid refresh token' });
  }
}

/**
 * Changes landlord password after verifying old password.
 */
export async function changePasswordHandler(req: AuthenticatedRequest, res: Response): Promise<void> {
  if (!req.user) {
    res.status(401).json({ error: 'Unauthorized' });
    return;
  }

  const parsed = changePasswordSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const { oldPassword, newPassword } = parsed.data;
  const [user] = await db.select().from(users).where(eq(users.id, req.user.userId)).limit(1);

  if (!user) {
    res.status(404).json({ error: 'User not found' });
    return;
  }

  const isOldValid = await verifyPassword(oldPassword, user.passwordHash);
  if (!isOldValid) {
    res.status(400).json({ error: 'Current password does not match' });
    return;
  }

  const newHash = await hashPassword(newPassword);
  await db
    .update(users)
    .set({ passwordHash: newHash, refreshTokenHash: null, updatedAt: new Date() })
    .where(eq(users.id, user.id));

  res.status(200).json({ message: 'Password updated successfully. Please re-login on all devices.' });
}
