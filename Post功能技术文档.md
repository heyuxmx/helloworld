# ZhuDeApp Post功能技术文档

## 1. 项目概述

ZhuDeApp 是一个私密情侣社交 Android 应用，Post（动态）功能是核心功能之一。用户可以发布包含文字、图片和视频的动态，对动态进行点赞、评论等交互操作。

后端为自建 Node.js + Express + PostgreSQL 服务器，通过 ngrok 静态域名对外服务。

## 2. 组件架构

### 2.1 主要组件

#### 数据模型层
| 类 | 路径 | 说明 |
|---|---|---|
| `Post` | `data/Post.kt` | 动态数据模型 |
| `Comment` | `data/Comment.kt` | 评论数据模型 |
| `UserProfile` | `data/UserProfile.kt` | 用户资料数据模型 |

#### 网络与数据层
| 类 | 路径 | 说明 |
|---|---|---|
| `HeyuModule` | `di/HeyuModule.kt` | 后端 API 接口封装（Ktor HTTP Client） |
| `UploadManager` | `di/UploadManager.kt` | 后台上传管理器（视频上传） |
| `UserManager` | `di/UserManager.kt` | 用户身份管理 |
| `PostRepository` | `repository/PostRepository.kt` | 离线优先数据层（Room 缓存） |

#### UI 组件层
| 类 | 说明 |
|---|---|
| `PostFragment` | 主页面，显示动态列表 |
| `PostAdapter` | 动态列表适配器 |
| `PostImagesAdapter` | 媒体资源（图片/视频）网格适配器 |
| `ImagePagerAdapter` | 媒体资源全屏浏览适配器 |
| `CreatePostActivity` | 创建动态页面 |
| `PostImagePagerActivity` | 媒体资源全屏浏览页面 |

#### 工具层
| 类 | 说明 |
|---|---|
| `VideoUtils` | 视频压缩、时长检查、文件大小获取 |
| `VideoCacheManager` | 视频缓存管理，保存原始视频到相册 |
| `DateUtils` | UTC 时间格式化为本地化显示 |

## 3. 媒体显示机制

### 3.1 PostAdapter 中的媒体显示

所有媒体（图片和视频）统一通过 `PostImagesAdapter` 在网格中显示：

- 视频仅显示缩略图（Glide 加载第一帧），叠加播放按钮图标
- 点击缩略图跳转到 `PostImagePagerActivity` 全屏浏览
- 使用 GridLayoutManager 实现 3 列网格布局

### 3.2 媒体类型判断

`PostImagesAdapter` 根据 URL 中的文件扩展名区分图片和视频：

```kotlin
override fun getItemViewType(position: Int): Int {
    return if (mediaUris[position].contains(".mp4", ignoreCase = true)) {
        TYPE_VIDEO
    } else {
        TYPE_IMAGE
    }
}
```

### 3.3 全屏浏览（PostImagePagerActivity）

- ViewPager2 实现图片/视频滑动浏览
- 自动播放当前页面视频，暂停上一个页面视频
- 支持水平滑动调节播放进度、循环播放
- 长按可保存图片/视频到本地相册

## 4. 创建动态流程

### 4.1 CreatePostActivity 工作流程

1. **媒体选择**：`ActivityResultContracts.PickMultipleVisualMedia(20)` 选择图片和视频
2. **视频过滤**：自动过滤超过 30 分钟的视频
3. **发布分两条路径**：
   - **含视频**：交给 `UploadManager` 后台上传，Activity 立即返回
   - **纯图片**：在当前页面弹窗显示进度，并行压缩上传

### 4.2 纯图片上传流程

```
选择图片 → 并行压缩（HeyuModule.compressImage）→ 并行上传（HeyuModule.uploadPostImage）
→ 创建动态（HeyuModule.createPost）→ 短信通知对方
```

图片并行上传使用 `coroutineScope + async + awaitAll` 实现。

### 4.3 含视频上传流程（UploadManager）

```
UploadManager.enqueue() → 后台协程逐个处理：
  → 视频压缩（VideoUtils.compressVideoIfNeeded）
  → 上传（HeyuModule.uploadPostVideo）
  → 缓存原始视频（VideoCacheManager）
→ 创建动态（HeyuModule.createPost）
```

`UploadManager` 通过 `StateFlow<UploadState>` 暴露上传状态，主页面可观察并展示通知。

## 5. 图片处理

### 5.1 压缩算法（HeyuModule.compressImage）

| 参数 | 默认值 |
|---|---|
| 最大边长 | 1080px |
| JPEG 质量 | 72% |

处理流程：
1. 读取 EXIF 方向信息
2. 计算 `inSampleSize` 避免超大图片撑爆内存
3. 解码 → 旋转校正 → 按 maxDimension 精确缩放
4. 压缩为 JPEG 字节数组

## 6. 视频处理

### 6.1 压缩算法（VideoUtils.compressVideoIfNeeded）

| 条件 | 行为 |
|---|---|
| 文件 < 40MB | 跳过压缩 |
| 文件 ≥ 40MB | 压缩至 ~38MB 目标 |

分辨率自动降级策略：

| 计算比特率 | 目标分辨率 |
|---|---|
| ≥ 1.5Mbps | 540p |
| ≥ 800Kbps | 480p |
| < 800Kbps | 360p（保底 400Kbps） |

使用 `com.otaliastudios:transcoder` 库进行视频重编码，帧率降至 25fps。

## 7. 后端 API 交互

所有网络请求通过 `HeyuModule`（Ktor HTTP Client）发起，服务器地址：

```
https://marth-nongerminative-hedonistically.ngrok-free.dev
```

### 7.1 动态相关 API

| 方法 | 说明 |
|---|---|
| `getPosts()` | GET `/api/posts` — 获取动态列表 |
| `createPost()` | POST `/api/posts` — 创建动态 |
| `deletePost()` | DELETE `/api/posts/:id` — 删除动态 |
| `likePost()` | POST `/api/posts/rpc/increment_likes` — 点赞 |

### 7.2 评论 API

| 方法 | 说明 |
|---|---|
| `addComment()` | POST `/api/comments` — 添加评论 |
| `deleteComment()` | DELETE `/api/comments/:id` — 删除评论 |

### 7.3 文件上传 API

| 方法 | 说明 |
|---|---|
| `uploadPostImage()` | POST `/api/storage/upload/post-images` — 上传图片 |
| `uploadPostVideo()` | POST `/api/storage/upload/post-images` — 上传视频 |
| `uploadAvatar()` | POST `/api/storage/upload/avatars` — 上传头像 |

上传使用 `MultiPartFormDataContent`，文件名为 UUID 生成。
上传后服务器返回公开访问 URL（`/storage/post-images/xxx` 或 `/storage/avatars/xxx`）。

## 8. 用户交互

### 8.1 动态操作
- 长按动态弹出删除确认对话框
- 点赞带震动反馈
- 评论支持添加和删除

### 8.2 媒体操作
- 长按图片/视频可保存到本地相册
- Android 10+ 使用 MediaStore，无需存储权限

### 8.3 界面交互
- 下拉刷新加载最新动态
- 发布成功后自动发送短信通知对方

## 9. 性能优化

- **图片加载**：Glide 内存管理 + OkHttp3 集成
- **视频压缩**：智能跳过小文件，分辨率自适应降级
- **并行上传**：纯图片场景使用协程并行上传
- **后台上传**：含视频时交给 UploadManager，不阻塞 UI
- **离线优先**：Room 本地缓存，网络恢复后自动同步
- **内存安全**：Bitmap 采样解码 + 及时 recycle
