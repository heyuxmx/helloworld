require('dotenv').config({ path: require('path').join(__dirname, '../.env') });

const express = require('express');
const cors = require('cors');
const path = require('path');

const postsRouter = require('./routes/posts');
const commentsRouter = require('./routes/comments');
const usersRouter = require('./routes/users');
const storageRouter = require('./routes/storage');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// 静态文件：上传的图片/视频/头像
app.use('/storage', express.static(path.join(__dirname, '../uploads')));

// 静态文件：APK 和版本检查 JSON
app.use('/app-releases', express.static(path.join(__dirname, '../public/releases')));

// API 路由
app.use('/api/posts', postsRouter);
app.use('/api/comments', commentsRouter);
app.use('/api/users', usersRouter);
app.use('/api/storage', storageRouter);

// 健康检查
app.get('/health', (req, res) => res.json({ status: 'ok', time: new Date().toISOString() }));

app.listen(PORT, () => {
  console.log(`✅ ZhuDe App Server 运行在端口 ${PORT}`);
  console.log(`🌐 外网地址: ${process.env.BASE_URL}`);
});
