/**
 * @file upload.routes.ts
 * @module modules/upload
 * @description Upload routes for profile pictures and Aadhaar cards.
 */

import { Router, Request, Response } from 'express';
import multer from 'multer';
import { requireAuth } from '../../middleware/auth.middleware';
import { uploadToCloudinary } from './upload.util';

export const uploadRouter = Router();

const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // Max 5MB
});

uploadRouter.post(
  '/',
  requireAuth,
  upload.single('file'),
  async (req: Request, res: Response): Promise<void> => {
    if (!req.file) {
      res.status(400).json({ error: 'No file uploaded' });
      return;
    }

    try {
      const fileUrl = await uploadToCloudinary(req.file.buffer, req.file.mimetype);
      res.status(200).json({
        message: 'File uploaded successfully',
        url: fileUrl,
      });
    } catch (error: any) {
      res.status(500).json({ error: error.message || 'File upload failed' });
    }
  }
);
