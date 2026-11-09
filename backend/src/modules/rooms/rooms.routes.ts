/**
 * @file rooms.routes.ts
 * @module modules/rooms
 * @description Routes for room operations protected by JWT auth.
 */

import { Router } from 'express';
import { getRoomsHandler, createRoomHandler, updateRoomHandler, deleteRoomHandler } from './rooms.controller';
import { requireAuth } from '../../middleware/auth.middleware';

export const roomsRouter = Router();

roomsRouter.use(requireAuth);

roomsRouter.get('/', getRoomsHandler);
roomsRouter.post('/', createRoomHandler);
roomsRouter.put('/:id', updateRoomHandler);
roomsRouter.delete('/:id', deleteRoomHandler);
