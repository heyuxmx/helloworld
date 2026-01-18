# ZhuDe App - 项目功能和设计文档

## 项目概述

ZhuDe App 是一款专为情侣设计的私密社交应用，主要功能包括动态分享、评论互动、图片上传、实时通知等。该应用采用 Android 原生开发，使用 Kotlin 语言，后端集成 Supabase 作为 BaaS 平台，实现数据存储和实时同步。

## 项目架构

### 技术栈
- **前端**: Android (Kotlin)
- **后端**: Supabase (PostgreSQL 数据库 + Real-time 功能)
- **云函数**: Supabase Edge Functions (用于推送通知)
- **状态管理**: Android Jetpack ViewModel + LiveData + Coroutines
- **UI 框架**: Android View System (非 Compose)
- **网络通信**: Ktor Client + Supabase SDK
- **图片处理**: Glide + Android Image Cropper
- **通知服务**: Firebase Cloud Messaging (FCM)

### 架构模式
- **MVVM (Model-View-ViewModel)**: 采用 MVVM 架构分离业务逻辑和 UI 层
- **Repository 模式**: 通过 SupabaseModule 统一管理数据访问层
- **单例模式**: UserManager 管理当前用户状态

## 核心功能模块

### 1. 用户管理系统

#### 功能描述
- 支持两位用户的切换选择（"高猪猪" 和 "徐大王"）
- 用户信息包括用户名、头像等
- 用户状态持久化存储

#### 设计细节
- 使用 `UserManager` 单例管理当前用户状态
- 通过 `SharedPreferences` 存储用户选择
- 用户ID硬编码为 "12345" (高猪猪) 和 "67890" (徐大王)

### 2. 动态发布系统

#### 功能描述
- 发布纯文字动态
- 发布带图片的动态（支持多图）
- 图片压缩和优化处理
- 实时上传至 Supabase Storage

#### 设计细节
- `CreatePostActivity` 负责动态创建界面
- 支持批量选择图片（最多20张）
- 图片自动压缩至最大1080像素，质量75%
- 自动处理图片EXIF方向信息，确保正确显示
- 上传成功后发送短信通知对方用户

### 3. 动态展示系统

#### 功能描述
- 以时间线形式展示所有动态
- 显示动态内容、作者、时间戳、点赞数、评论数
- 支持多图网格显示
- 实现下拉刷新功能

#### 设计细节
- 使用 `RecyclerView` + `PostAdapter` 实现动态列表
- `PostFragment` 负责动态列表的展示
- 实现智能时间戳显示（如"刚刚"、"X分钟前"、"昨天"等）
- 图片网格使用3列GridLayoutManager

### 4. 评论互动系统

#### 功能描述
- 对动态进行评论
- 查看动态下的所有评论
- 删除自己的评论
- 评论草稿保存功能

#### 设计细节
- 底部浮动评论框，点击动态可快速回复
- 评论内容本地缓存，避免输入丢失
- 支持长按删除评论（仅限自己发布的评论）

### 5. 点赞系统

#### 功能描述
- 为动态点赞
- 实现实时点赞数更新
- 本地状态同步（乐观UI）

#### 设计细节
- 点赞按钮有震动反馈
- 点击后立即更新UI，再异步更新服务器
- 使用 Supabase RPC 函数 `increment_likes` 处理点赞逻辑

### 6. 图片管理功能

#### 功能描述
- 动态图片预览（支持滑动浏览）
- 图片长按保存到本地相册
- 用户头像上传和更新

#### 设计细节
- 使用 PhotoView 实现图片手势缩放
- 保存图片需要存储权限（Android 10以下需要 WRITE_EXTERNAL_STORAGE 权限）
- 头像上传使用圆形裁剪功能

### 7. 通知系统

#### 功能描述
- 新动态发布时发送推送通知给对方
- 支持短信通知（作为备用方案）
- 通知点击可跳转到对应动态

#### 设计细节
- Supabase Row Level Security (RLS) 触发器调用 Edge Function
- Edge Function 使用 Google Service Account 向 FCM 发送通知
- 通知携带参数可在应用内直接跳转到指定动态

### 8. 个性化功能

#### 功能描述
- 用户可以修改用户名
- 更换个人头像
- 计算恋爱天数显示

#### 设计细节
- 头像更换使用 CircleImageView 显示圆形头像
- 恋爱天数从2024年12月2日开始计算
- 用户名和头像信息实时同步到 Supabase 数据库

## 数据模型

### Post（动态）
```kotlin
data class Post(
    val content: String,        // 动态内容
    val userId: String,         // 作者ID
    val imageUrls: List<String>, // 图片URL列表
    val id: Long = 0,           // 动态ID
    val createdAt: String,      // 创建时间
    var likes: Int = 0,         // 点赞数
    val comments: MutableList<Comment>, // 评论列表
    val author: UserProfile?    // 作者信息
)
```

### Comment（评论）
```kotlin
data class Comment(
    val id: Long,               // 评论ID
    val postId: Long,           // 所属动态ID
    val userId: String,         // 评论者ID
    val content: String,        // 评论内容
    val createdAt: String?,     // 创建时间
    val author: UserProfile?    // 评论者信息
)
```

### UserProfile（用户资料）
```kotlin
data class UserProfile(
    val id: String,             // 用户ID
    val username: String?,      // 用户名
    val avatarUrl: String?      // 头像URL
)
```

## UI/UX 设计特色

### 界面布局
- 主界面采用 DrawerLayout + BottomNavigationView 的混合导航模式
- 顶部 Toolbar 提供统一的操作栏
- 启动页采用 1 秒闪屏页设计

### 导航结构
- **首页**: WelcomeFragment（欢迎动画）
- **记录**: PostFragment（动态时间线）
- **嘻嘻**: DatecountFragment（恋爱天数等）
- **我的**: MineFragment（个人中心）

### 交互体验
- 评论输入框采用底部弹出设计，智能适配软键盘
- 点赞操作有触觉反馈（震动）
- 图片支持手势缩放和滑动浏览
- 下拉刷新和滚动加载更多

## 安全与权限

### 权限需求
- `INTERNET`: 网络通信
- `SEND_SMS`: 发送通知短信
- `WRITE_EXTERNAL_STORAGE`: 保存图片到相册（Android 10以下）
- `VIBRATE`: 触觉反馈
- `POST_NOTIFICATIONS`: 通知权限

### 数据安全
- 使用 Supabase Row Level Security (RLS) 控制数据访问权限
- 敏感操作（如删除）需要二次确认
- 用户数据隔离，只能操作自己的内容

## 后端架构

### 数据库设计
- `users` 表：存储用户信息（ID、用户名、头像URL、FCM Token）
- `posts` 表：存储动态信息（内容、作者、图片URL、创建时间、点赞数）
- `comments` 表：存储评论信息（内容、作者、所属动态、创建时间）

### 云函数
- `send-notification`: 新动态发布时自动触发，向对方发送推送通知
- 使用 Google Service Account 认证 FCM 服务

### 存储系统
- `post-images` bucket: 存储动态图片
- `avatars` bucket: 存储用户头像

## 部署与维护

### 版本更新机制
- 应用启动时检查更新
- 从 Supabase Storage 获取更新配置文件
- 支持强制更新提示

### 错误处理
- 全局异常捕获和用户友好提示
- 网络请求失败重试机制
- 数据库操作异常处理

## 项目特点总结

1. **私密性**: 专门为双人关系设计，数据完全私密
2. **实时性**: 集成实时通知，及时获取动态更新
3. **美观性**: 精美的UI设计，良好的用户体验
4. **功能性**: 完整的社交功能（发布、评论、点赞、图片）
5. **可靠性**: 完善的错误处理和数据一致性保证
6. **个性化**: 支持头像、昵称自定义，恋爱天数计算

该项目体现了开发者对情侣用户群体需求的深入理解，以及在移动应用开发方面的技术实力。