/**
 * @file rooms.controller.ts
 * @module modules/rooms
 * @description CRUD endpoints for rooms management including occupancy tracking and deletion safeguards.
 */

import { Request, Response } from 'express';
import { z } from 'zod';
import { db } from '../../db';
import { rooms, tenants } from '../../db/schema';
import { eq } from 'drizzle-orm';

const createRoomSchema = z.object({
  roomNo: z.string().min(1, 'Room number is required'),
  description: z.string().optional(),
  floor: z.number().int().default(0),
  attachments: z.array(z.string()).optional(),
  basePrice: z.number().nonnegative('Base price must be positive or zero'),
});

const updateRoomSchema = createRoomSchema.partial();

/**
 * Returns all rooms ordered by room number.
 */
export async function getRoomsHandler(_req: Request, res: Response): Promise<void> {
  const allRooms = await db.select().from(rooms).orderBy(rooms.roomNo);
  res.status(200).json({ rooms: allRooms });
}

/**
 * Creates a new room.
 */
export async function createRoomHandler(req: Request, res: Response): Promise<void> {
  const parsed = createRoomSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const { roomNo, description, floor, attachments, basePrice } = parsed.data;

  // Check if room number already exists
  const [existing] = await db.select().from(rooms).where(eq(rooms.roomNo, roomNo)).limit(1);
  if (existing) {
    res.status(409).json({ error: `Room ${roomNo} already exists` });
    return;
  }

  const [newRoom] = await db
    .insert(rooms)
    .values({
      roomNo,
      description,
      floor,
      attachments: attachments || [],
      basePrice: basePrice.toFixed(2),
      isOccupied: false,
    })
    .returning();

  res.status(201).json({ message: 'Room created successfully', room: newRoom });
}

/**
 * Updates room details.
 */
export async function updateRoomHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;
  const parsed = updateRoomSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation failed', details: parsed.error.format() });
    return;
  }

  const updateData: Record<string, any> = { ...parsed.data, updatedAt: new Date() };
  if (parsed.data.basePrice !== undefined) {
    updateData.basePrice = parsed.data.basePrice.toFixed(2);
  }

  const [updated] = await db.update(rooms).set(updateData).where(eq(rooms.id, id)).returning();

  if (!updated) {
    res.status(404).json({ error: 'Room not found' });
    return;
  }

  res.status(200).json({ message: 'Room updated', room: updated });
}

/**
 * Deletes room. Fails if occupied by an active tenant.
 */
export async function deleteRoomHandler(req: Request, res: Response): Promise<void> {
  const { id } = req.params;

  // Check if room has active tenant
  const [activeTenant] = await db
    .select()
    .from(tenants)
    .where(eq(tenants.roomId, id))
    .limit(1);

  if (activeTenant && activeTenant.isActive) {
    res.status(400).json({
      error: `Cannot delete room: Currently occupied by tenant ${activeTenant.name}`,
    });
    return;
  }

  const [deleted] = await db.delete(rooms).where(eq(rooms.id, id)).returning();
  if (!deleted) {
    res.status(404).json({ error: 'Room not found' });
    return;
  }

  res.status(200).json({ message: 'Room deleted successfully', room: deleted });
}
