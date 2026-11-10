/**
 * @file upload.util.ts
 * @module modules/upload
 * @description Cloudinary storage adapter with safe local fallback.
 */

import { v2 as cloudinary } from 'cloudinary';
import { env } from '../../config/env';

if (env.CLOUDINARY_CLOUD_NAME && env.CLOUDINARY_API_KEY && env.CLOUDINARY_API_SECRET) {
  cloudinary.config({
    cloud_name: env.CLOUDINARY_CLOUD_NAME,
    api_key: env.CLOUDINARY_API_KEY,
    api_secret: env.CLOUDINARY_API_SECRET,
    secure: true,
  });
}

/**
 * Uploads a file buffer to Cloudinary or falls back to data URI if credentials are not set.
 * @param buffer File buffer from Multer.
 * @param mimetype File mime type (image/jpeg, etc.)
 * @returns Public URL of the uploaded image.
 */
export async function uploadToCloudinary(buffer: Buffer, mimetype: string): Promise<string> {
  if (env.CLOUDINARY_CLOUD_NAME && env.CLOUDINARY_API_KEY && env.CLOUDINARY_API_SECRET) {
    return new Promise((resolve, reject) => {
      const uploadStream = cloudinary.uploader.upload_stream(
        { folder: '10nts_documents', resource_type: 'image' },
        (error, result) => {
          if (error || !result) {
            reject(error || new Error('Cloudinary upload returned empty result'));
          } else {
            resolve(result.secure_url);
          }
        }
      );
      uploadStream.end(buffer);
    });
  }

  // Safe fallback if Cloudinary credentials are not populated in .env
  const base64Data = buffer.toString('base64');
  return `data:${mimetype};base64,${base64Data}`;
}
