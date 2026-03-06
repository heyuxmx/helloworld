const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const ffmpeg = require('fluent-ffmpeg');
const ffmpegPath = require('ffmpeg-static');
ffmpeg.setFfmpegPath(ffmpegPath);

const UPLOADS_DIR = path.join(__dirname, '../../uploads');
const ALLOWED_BUCKETS = ['post-images', 'avatars'];

// 确保上传目录存在
ALLOWED_BUCKETS.forEach((bucket) => {
  const dir = path.join(UPLOADS_DIR, bucket);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const bucket = req.params.bucket;
    if (!ALLOWED_BUCKETS.includes(bucket)) {
      return cb(new Error('Invalid bucket'));
    }
    cb(null, path.join(UPLOADS_DIR, bucket));
  },
  filename: (req, file, cb) => {
    // 客户端通过 multipart field "filename" 传来 UUID 文件名
    const filename = req.body.filename || `${Date.now()}-${file.originalname}`;
    cb(null, filename);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100MB 上限
});

// POST /api/storage/upload/:bucket — 上传文件，返回公开 URL
// 若上传的是视频，额外生成 _thumb.jpg 缩略图（取第1秒帧）
router.post('/upload/:bucket', upload.single('file'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded' });

  const bucket = req.params.bucket;
  const publicUrl = `${process.env.BASE_URL}/storage/${bucket}/${req.file.filename}`;

  const isVideo = req.file.mimetype && req.file.mimetype.startsWith('video/');
  if (isVideo) {
    const thumbFilename = req.file.filename.replace(/\.mp4$/i, '_thumb.jpg');
    const thumbPath = path.join(UPLOADS_DIR, bucket, thumbFilename);
    try {
      await new Promise((resolve, reject) => {
        ffmpeg(req.file.path)
          .screenshots({
            timestamps: ['00:00:01'],
            filename: thumbFilename,
            folder: path.join(UPLOADS_DIR, bucket),
            size: '480x?',
          })
          .on('end', resolve)
          .on('error', (err) => {
            // 若第1秒取帧失败（视频较短），退回取第0秒
            ffmpeg(req.file.path)
              .screenshots({
                timestamps: ['00:00:00.001'],
                filename: thumbFilename,
                folder: path.join(UPLOADS_DIR, bucket),
                size: '480x?',
              })
              .on('end', resolve)
              .on('error', reject);
          });
      });
    } catch (e) {
      console.error('缩略图生成失败（非致命）:', e.message);
    }
  }

  res.json({ url: publicUrl });
});

module.exports = router;
