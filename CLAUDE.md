# ZhuDe App — Claude 项目上下文

## 项目简介
专为两位固定用户（高猪猪 ID:`12345`，徐大王 ID:`67890`）设计的私密情侣社交 Android 应用。
当前后端为 **Supabase**，正在迁移至**本机自建服务器**。

## 技术栈
- **Android 客户端**: Kotlin · MVVM · Room · Ktor/Supabase SDK · Glide · FCM
- **当前后端**: Supabase (PostgreSQL + Postgrest + Storage + Edge Functions)
- **目标后端**: 自建本机服务器（技术栈待定，推荐 Node.js + Express + PostgreSQL）
- **推送通知**: Firebase Cloud Messaging (FCM)，Google Service Account 认证

## 关键文件路径
| 文件 | 说明 |
|------|------|
| `app/src/main/java/.../di/SupabaseModule.kt` | 所有后端调用的入口（迁移核心） |
| `app/src/main/java/.../repository/PostRepository.kt` | 离线优先数据层 |
| `app/src/main/java/.../database/AppDatabase.kt` | Room 本地缓存 |
| `supabase/functions/send-notification/index.ts` | 推送通知 Edge Function（需复现） |
| `app/build.gradle.kts` | 含 UPDATE_JSON_URL 版本检查地址 |

## 数据库表结构（Supabase PostgreSQL）
```sql
-- users 表
users(id TEXT PK, username TEXT, avatar_url TEXT, fcm_token TEXT, created_at TIMESTAMPTZ)

-- posts 表
posts(id BIGSERIAL PK, content TEXT, user_id TEXT FK→users.id,
      image_urls TEXT[], video_url TEXT, likes INT DEFAULT 0, created_at TIMESTAMPTZ)

-- comments 表
comments(id BIGSERIAL PK, post_id BIGINT FK→posts.id, user_id TEXT FK→users.id,
         content TEXT, created_at TIMESTAMPTZ)
```

## 需要复现的后端 API（替代 Supabase Postgrest）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /posts | 获取动态列表（含嵌套 author + comments） |
| POST | /posts | 创建动态 |
| DELETE | /posts/:id | 删除动态 |
| POST | /comments | 添加评论 |
| DELETE | /comments/:id | 删除评论 |
| GET | /users | 获取用户列表 |
| GET | /users/:id | 获取单个用户 |
| PATCH | /users/:id | 更新用户名或头像URL |
| PATCH | /users/:id/fcm-token | 更新 FCM Token |
| POST | /rpc/increment_likes | 点赞（原子递增） |
| POST | /storage/upload/post-images | 上传动态图片/视频 |
| POST | /storage/upload/avatars | 上传头像 |
| GET | /storage/public/:bucket/:file | 获取公开文件 URL |

## 推送通知逻辑（Edge Function 复现）
- 触发时机：新 Post 创建后（数据库触发器 or 业务层调用）
- 逻辑：查询接收方 fcm_token → 调用 FCM v1 API 发送通知
- 认证：Google Service Account（见 `supabase.env`）
- FCM Project: `zhudeapp-push`

## 版本更新检查
- 当前 URL: `https://bvgtzgxscnqhugjirgzp.supabase.co/storage/v1/object/public/app-releases/update-check_xiaogao.json`
- 迁移后需提供同等 JSON 文件的 HTTP 接口

## 已确认的架构决策
- **网络方案**: 花生壳免费版内网穿透，本机安装花生壳客户端，手机端零依赖
- **服务器地址**: `http://iw12081mm7266.vicp.fun:18397`（花生壳外网地址，本机 3000 端口映射）
- **带宽**: 免费版 1Mbps，图片/文字流畅，视频上传较慢（40MB约5分钟）
- **文件存储**: Express 本地文件存储（`server/uploads/`），通过 HTTP 静态文件服务对外访问
- **APK 托管**: `server/public/releases/`，版本检查 JSON 也由本机服务器提供
- **阿里云 OSS SDK**: 已从 `build.gradle.kts` 中移除（遗留依赖）

## 迁移注意事项
- Android 客户端修改核心：将 `SupabaseModule.kt` 中的 SDK 调用替换为 Ktor HTTP 客户端调用
- 图片 URL 从 Supabase CDN 域名切换为 Tailscale 虚拟 IP（需考虑已有历史数据）
- 用户 ID 硬编码，无需实现注册/登录认证系统
- Room 本地缓存层无需修改，只需改网络数据源
- Ktor Client 已在依赖中（`ktor-client-android:2.3.11`），无需新增依赖

## MCP 服务器
- **playwright**: 浏览器自动化（调试/测试）
- **postgres**: 直连本地 PostgreSQL 数据库（后端搭建后生效）
