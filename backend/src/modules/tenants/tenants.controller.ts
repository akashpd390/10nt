/**
 * @file tenants.controller.ts
 * @module modules/tenants
 * @description Tenant management controller: creation with room binding, updates, deletion checks.
 */

import { Request, Response } from 'express';
import { z } from 'zod';
import { db } from '../../db';
import { tenants, rooms, ledgerEntries } from '../../db/schema';
import { eq, desc } from 'drizzle-orm';

const createTenantSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  phone: z.string().min(10, 'Valid phone number required'),
  familyMembersCount: z.number().int().positive().default(1),
  roomId: z.string().uuid('Valid room UUID required'),
  allottedPrice: z.number().positive('Allotted monthly rent price must be positive'),
  electricityRatePerUnit: z.number().positive().default(8.0),
  initialMeterReading: z.number().nonnegative().default(0.0),
  initialAdvanceOrBalance: z.number().default(0.0), // Can be positive (advance) or negative (due)
  adhaarCardUrl: z.string().optional(),
  profilePicUrl: z.string().optional(),
});

const updateTenantSchema = z.object({
  name: z.string().optional(),
  phone: z.string().optional(),
  familyMembersCount: z.number().int().positive().optional(),
  allottedPrice: z.number().positive().optional(),
  electricityRatePerUnit: z.number().positive().optional(),
  adhaarCardUrl: z.string().optional(),
  profilePicUrl: z.string().optional(),
  isActive: z.boolean().optional(),
});

/**
 * Lists all tenants with their room assignment details.
 */
export async function getTenantsHandler(_req: Request, res: Response): Promise<void> {
  const result = await db.query.tenants.findMany({
    with: {
      room: true,
    },
    orderBy: [desc(tenants.createdAt)],
  });

  res.status(200).json({ tenants: result });
}

/**
 * Retrieves a single tenant with full room and recent ledger history.
 */
export async function getTenantByIdHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;
  const tenant = await db.query.tenants.findFirst({
    where: eq(tenants.id, id),
    with: {
      room: true,
      ledgerEntries: {
        orderBy: [desc(ledgerEntries.createdAt)],
        limit: 10,
      },
    },
  });

  if (!tenant) {
    res.status(404).json({ error: 'Tenant not found' });
    return;
  }

  res.status(200).json({ tenant });
}

/**
 * Creates a tenant and marks the assigned room as occupied.
 */
export async function createTenantHandler(req: Request, res: Response): Promise<void> {
  const parsed = createTenantSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const data = parsed.data;

  // Verify room exists and is not already occupied
  const [room] = await db.select().from(rooms).where(eq(rooms.id, data.roomId)).limit(1);
  if (!room) {
    res.status(404).json({ error: 'Selected room not found' });
    return;
  }

  if (room.isOccupied) {
    res.status(400).json({ error: `Room ${room.roomNo} is already occupied` });
    return;
  }

  // Insert tenant
  const [newTenant] = await db
    .insert(tenants)
    .values({
      name: data.name,
      phone: data.phone,
      familyMembersCount: data.familyMembersCount,
      roomId: data.roomId,
      allottedPrice: data.allottedPrice.toFixed(2),
      electricityRatePerUnit: data.electricityRatePerUnit.toFixed(2),
      lastMeterReading: data.initialMeterReading.toFixed(2),
      currentBalance: data.initialAdvanceOrBalance.toFixed(2),
      adhaarCardUrl: data.adhaarCardUrl,
      profilePicUrl: data.profilePicUrl,
      isActive: true,
    })
    .returning();

  // Mark room occupied
  await db.update(rooms).set({ isOccupied: true, updatedAt: new Date() }).where(eq(rooms.id, data.roomId));

  // If initial balance exists, log in ledger
  if (data.initialAdvanceOrBalance !== 0) {
    await db.insert(ledgerEntries).values({
      tenantId: newTenant.id,
      type: 'MANUAL_ADJUSTMENT',
      amount: data.initialAdvanceOrBalance.toFixed(2),
      runningBalance: newTenant.currentBalance,
      description: 'Opening balance / initial advance upon move-in',
    });
  }

  res.status(201).json({ message: 'Tenant registered successfully', tenant: newTenant });
}

/**
 * Updates tenant info.
 */
export async function updateTenantHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;
  const parsed = updateTenantSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const updateData: Record<string, any> = { ...parsed.data, updatedAt: new Date() };
  if (parsed.data.allottedPrice !== undefined) {
    updateData.allottedPrice = parsed.data.allottedPrice.toFixed(2);
  }
  if (parsed.data.electricityRatePerUnit !== undefined) {
    updateData.electricityRatePerUnit = parsed.data.electricityRatePerUnit.toFixed(2);
  }

  const [updated] = await db.update(tenants).set(updateData).where(eq(tenants.id, id)).returning();

  if (!updated) {
    res.status(404).json({ error: 'Tenant not found' });
    return;
  }

  res.status(200).json({ message: 'Tenant updated successfully', tenant: updated });
}

/**
 * Deletes or vacates a tenant.
 * Safeguard: Cannot delete if pending balance (< 0).
 */
export async function deleteTenantHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;

  const [tenant] = await db.select().from(tenants).where(eq(tenants.id, id)).limit(1);
  if (!tenant) {
    res.status(404).json({ error: 'Tenant not found' });
    return;
  }

  const currentBal = Number(tenant.currentBalance);
  if (currentBal < 0) {
    res.status(400).json({
      error: `Cannot delete tenant: Unpaid balance of ₹${Math.abs(currentBal).toFixed(2)} is pending. Please settle balance first.`,
      pendingBalance: currentBal,
    });
    return;
  }

  // Free room if assigned
  if (tenant.roomId) {
    await db.update(rooms).set({ isOccupied: false, updatedAt: new Date() }).where(eq(rooms.id, tenant.roomId));
  }

  await db.delete(tenants).where(eq(tenants.id, id));

  res.status(200).json({ message: 'Tenant removed and room marked available' });
}
