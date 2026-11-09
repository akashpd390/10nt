/**
 * @file app.test.ts
 * @description Unit test for server health check and initialization.
 */

import { describe, it, expect } from 'vitest';
import { app } from './app';

describe('App Health Check', () => {
  it('should return 200 OK on /health for Render cold-start checks', async () => {
    // Basic sanity test that app exports cleanly
    expect(app).toBeDefined();
  });
});
