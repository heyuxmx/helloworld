const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');

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
router.post('/upload/:bucket', upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded' });

  const publicUrl = `${process.env.BASE_URL}/storage/${req.params.bucket}/${req.file.filename}`;
  res.json({ url: publicUrl });
});

module.exports = router;
