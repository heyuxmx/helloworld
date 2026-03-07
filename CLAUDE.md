# ZhuDe App — Claude 项目上下文

## 项目简介
专为两位固定用户设计的私密情侣社交 Android 应用。
后端为自建 Node.js + Express + PostgreSQL 服务器，通过 ngrok 隧道对外服务。

## 技术栈
- **Android 客户端**: Kotlin · MVVM · Room · Ktor HTTP Client · Glide
- **后端**: Node.js + Express + PostgreSQL（`server/` 目录）
- **公网隧道**: ngrok 静态域名（已安装为 Windows 服务）
- **推送通知**: 服务端通过 Google Service Account 调用 FCM v1 API

## 用户系统（硬编码，无需 Auth）
- 高猪猪: 数据库 ID = `"12345"`，本地 SharedPreferences key = `"user_gaobao"`
- 徐大王: 数据库 ID = `"67890"`，本地 SharedPreferences key = `"user_xubaba"`
- `UserManager.kt` 使用数据库 ID 与服务器通信
- `Users.kt` (model) 使用本地 key 管理 SharedPreferences

## 关键文件路径
| 文件 | 说明 |
|------|------|
| `app/src/main/java/.../di/HeyuModule.kt` | 所有后端调用的入口 |
| `app/src/main/java/.../repository/PostRepository.kt` | 离线优先数据层 |
| `app/src/main/java/.../database/AppDatabase.kt` | Room 本地缓存 |
| `server/src/services/notification.js` | 推送通知服务（FCM v1） |
| `app/build.gradle.kts` | 含 UPDATE_JSON_URL 版本检查地址 |

## 数据库表结构（PostgreSQL）
```sql
users(id TEXT PK, username TEXT, avatar_url TEXT, fcm_token TEXT, created_at TIMESTAMPTZ)
posts(id BIGSERIAL PK, content TEXT, user_id TEXT FK→users.id,
      image_urls TEXT[], video_url TEXT, likes INT DEFAULT 0, created_at TIMESTAMPTZ)
comments(id BIGSERIAL PK, post_id BIGINT FK→posts.id, user_id TEXT FK→users.id,
         content TEXT, created_at TIMESTAMPTZ)
```

## 后端 API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/posts | 获取动态列表（含嵌套 author + comments） |
| POST | /api/posts | 创建动态 |
| DELETE | /api/posts/:id | 删除动态 |
| POST | /api/comments | 添加评论 |
| DELETE | /api/comments/:id | 删除评论 |
| GET | /api/users | 获取用户列表 |
| GET | /api/users/:id | 获取单个用户 |
| PATCH | /api/users/:id | 更新用户名或头像URL |
| PATCH | /api/users/:id | 更新 FCM Token |
| POST | /api/posts/rpc/increment_likes | 点赞（原子递增） |
| POST | /api/storage/upload/post-images | 上传动态图片/视频 |
| POST | /api/storage/upload/avatars | 上传头像 |

## 架构决策
- **服务器地址**: `https://marth-nongerminative-hedonistically.ngrok-free.dev`（本机 3000 端口映射）
- **文件存储**: Express 本地文件存储（`server/uploads/`），通过 `/storage/` 路由对外访问
- **APK 托管**: `server/public/releases/`，版本检查 JSON 由本机服务器提供
- **用户 ID 硬编码**，无需注册/登录认证系统
- **Room 本地缓存层** 提供离线优先体验

## MCP 服务器
- **playwright**: 浏览器自动化（调试/测试）
- **postgres**: 直连本地 PostgreSQL 数据库（`postgresql://localhost:5432/zhudedb`）
