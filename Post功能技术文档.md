# ZhuDeApp Post功能技术文档

## 1. 项目概述

ZhuDeApp是一个Android社交应用，其中Post（动态）功能是核心功能之一。用户可以发布包含文字、图片和视频的动态，其他用户可以对动态进行点赞、评论等交互操作。

## 2. Post相关组件架构

### 2.1 主要组件构成

#### 数据模型层
- **Post**: 核心动態数据模型
- **Comment**: 评论数据模型
- **UserProfile**: 用户资料数据模型

#### UI组件层
- **PostFragment**: 主页面Fragment，负责显示动态列表
- **PostAdapter**: 动态列表适配器
- **PostImagesAdapter**: 媒体资源（图片/视频）网格适配器
- **ImagePagerAdapter**: 媒体资源全屏浏览适配器
- **CreatePostActivity**: 创建动态页面
- **PostImagePagerActivity**: 媒体资源全屏浏览页面

#### 工具层
- **VideoUtils**: 视频处理工具类
- **SupabaseModule**: 后端服务接口（数据存储、上传等）

## 3. 动态图片视频排版机制

### 3.1 PostAdapter中的媒体显示逻辑

在PostAdapter的bind方法中，实现了统一的媒体显示逻辑：

```kotlin
// --- UNIFIED MEDIA DISPLAY LOGIC ---
if (post.imageUrls.isNotEmpty()) {
    mediaContainer.visibility = View.VISIBLE
    
    // Check if there's a mix of videos and images or just one type
    val hasVideo = post.imageUrls.any { it.contains(".mp4", ignoreCase = true) }
    val hasImage = post.imageUrls.any { !it.contains(".mp4", ignoreCase = true) }
    
    if (hasVideo && hasImage) {
        // Mixed content - show in grid layout with both videos and images
        setupMediaGrid(post.imageUrls)
        imagesRecyclerView.visibility = View.VISIBLE
        postVideoContainer.visibility = View.GONE
    } else if (hasVideo && post.imageUrls.size == 1) {
        // Single video - show in grid layout (now showing thumbnail)
        setupMediaGrid(post.imageUrls)
        imagesRecyclerView.visibility = View.VISIBLE
        postVideoContainer.visibility = View.GONE
    } else {
        // Only images or multiple videos - show in grid layout
        setupMediaGrid(post.imageUrls)
        imagesRecyclerView.visibility = View.VISIBLE
        postVideoContainer.visibility = View.GONE
    }
} else {
    mediaContainer.visibility = View.GONE
}
```

### 3.2 PostImagesAdapter媒体类型判断

PostImagesAdapter根据文件扩展名判断媒体类型：

```kotlin
override fun getItemViewType(position: Int): Int {
    return if (mediaUris[position].contains(".mp4", ignoreCase = true)) {
        TYPE_VIDEO
    } else {
        TYPE_IMAGE
    }
}
```

### 3.3 布局文件结构

#### list_item_post.xml
- 包含完整的动态布局结构
- 媒体容器(media_container)包含：
  - images_recycler_view: 用于显示图片/视频网格
  - post_video_container: 用于显示单个视频（已弃用）

#### list_item_image.xml 和 list_item_video.xml
- 使用SquareImageView确保正方形显示
- 视频项额外包含播放按钮和进度条

## 4. 创建帖子的逻辑

### 4.1 CreatePostActivity工作流程

1. **媒体选择**：使用ActivityResultContracts.PickMultipleVisualMedia选择多张图片和视频
2. **视频长度检查**：过滤超过30分钟的视频
3. **发布逻辑**：
   - 检查内容和媒体是否为空
   - 逐个上传媒体文件
   - 创建动态记录

### 4.2 上传流程

```kotlin
private suspend fun uploadImages(): List<String> {
    val resultUrls = mutableListOf<String>()
    val total = selectedImageUris.size
    
    for ((index, uri) in selectedImageUris.withIndex()) {
        val countInfo = "(${index + 1}/$total)"
        val mimeType = contentResolver.getType(uri)
        val isVideo = mimeType?.startsWith("video/") == true
        
        var processedUri = uri
        if (isVideo) {
            // 压缩视频
            updateDialog("视频压缩中 $countInfo...", 0)
            processedUri = VideoUtils.compressVideoIfNeeded(this, uri) { progress ->
                updateDialog("视频压缩中 $countInfo...", progress)
            }
        }

        // 上传文件
        updateDialog("正在上传 $countInfo...", 30)
        
        val fileBytes = withContext(Dispatchers.IO) {
            if (isVideo) {
                VideoUtils.uriToByteArrayWithLimit(this@CreatePostActivity, processedUri)
            } else {
                SupabaseModule.compressImage(this@CreatePostActivity, uri)
            }
        }

        val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
        val url = withContext(Dispatchers.IO) {
            if (isVideo) {
                SupabaseModule.uploadPostVideo(fileBytes, fileName)
            } else {
                SupabaseModule.uploadPostImage(fileBytes, fileName)
            }
        }
        resultUrls.add(url)
        updateDialog("上传完成 $countInfo", 100)
    }
    return resultUrls
}
```

## 5. 图片和视频处理机制

### 5.1 图片处理

#### 压缩算法
- **目标尺寸**：最大边不超过1080像素
- **压缩质量**：75%
- **EXIF方向处理**：自动纠正图片旋转方向
- **采样率计算**：根据原图尺寸和目标尺寸计算合适的采样率

```kotlin
fun compressImage(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1080,
    quality: Int = 75
): ByteArray {
    // 处理EXIF方向
    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    // 计算缩放比例
    var inSampleSize = 1f
    if (srcHeight > maxDimension || srcWidth > maxDimension) {
        inSampleSize = if (srcWidth > srcHeight) {
            srcWidth / maxDimension
        } else {
            srcHeight / maxDimension
        }
    }

    // 使用Matrix进行缩放和旋转
    matrix.postScale(1/inSampleSize, 1/inSampleSize)

    // 压缩为JPEG
    val scaledBitmap = context.contentResolver.openInputStream(uri)?.use {
        val sourceBitmap = BitmapFactory.decodeStream(it)
        Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    scaledBitmap?.recycle()

    return outputStream.toByteArray()
}
```

### 5.2 视频处理

#### 压缩算法
- **文件大小检查**：如果视频小于40MB，跳过压缩
- **目标文件大小**：约38MB（为音频和容器开销预留空间，确保最终输出在40MB以内）
- **分辨率调整**：根据比特率自动调整分辨率
- **帧率调整**：降低至25帧以节省空间
- **编码策略**：使用DefaultVideoStrategy进行视频重编码

```kotlin
suspend fun compressVideoIfNeeded(
    context: Context, 
    inputUri: Uri, 
    onProgress: (Int) -> Unit = {}
): Uri {
    val fileSize = getVideoFileSize(context, inputUri)
    // 如果视频小于 40MB，直接跳过压缩以节省时间
    if (fileSize in 1..40_000_000L) return inputUri

    val durationMs = getVideoDuration(context, inputUri)
    if (durationMs <= 0) return inputUri

    // 目标设为 38MB，预留足够空间给音频和容器开销，确保最终输出在 40MB 以内
    val targetSizeBytes = 38_000_000L
    val durationSec = durationMs / 1000.0
    
    // 计算压缩后的理论比特率 (bps)
    var targetBitrate = ((targetSizeBytes * 8) / durationSec).toInt()
    
    // 分辨率自动降级：保证在低比特率下画面不模糊
    val targetHeight = when {
        targetBitrate >= 1_500_000 -> 540  
        targetBitrate >= 800_000 -> 480
        else -> {
            targetBitrate = targetBitrate.coerceAtLeast(400_000) // 保底比特率
            360 
        }
    }

    val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")

    return try {
        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            val strategy = DefaultVideoStrategy.Builder()
                .bitRate(targetBitrate.toLong())
                .frameRate(25) // 降为 25 帧以节省空间
                .addResizer(AtMostResizer(targetHeight))
                .build()

            Transcoder.into(outputFile.path)
                .addDataSource(context, inputUri)
                .setVideoTrackStrategy(strategy)
                .setListener(object : TranscoderListener {
                    override fun onTranscodeProgress(progress: Double) {
                        onProgress((progress * 100).toInt())
                    }
                    override fun onTranscodeCompleted(successCode: Int) {
                        if (continuation.isActive) continuation.resume(true)
                    }
                    override fun onTranscodeCanceled() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                    override fun onTranscodeFailed(exception: Throwable) {
                        Log.e(TAG, "Transcode failed", exception)
                        if (continuation.isActive) continuation.resume(false)
                    }
                }).transcode()
        }
        if (success && outputFile.exists()) {
            val newSize = outputFile.length()
            Log.d(TAG, "压缩完成，新大小: ${newSize / 1024} KB")
            Uri.fromFile(outputFile)
        } else {
            inputUri
        }
    } catch (e: Exception) {
        Log.e(TAG, "Compression error", e)
        inputUri
    }
}
```

## 6. 视频播放控制

### 6.1 列表页视频显示
- 仅显示视频缩略图，不进行播放
- 使用Glide加载视频第一帧作为缩略图
- 点击缩略图或播放按钮跳转到全屏播放页面

### 6.2 全屏播放页面（PostImagePagerActivity）
- 使用ViewPager2实现图片/视频滑动浏览
- 自动播放当前页面的视频
- 暂停上一个页面的视频
- 提供完整的播放控制（播放/暂停、进度条、拖拽控制）

### 6.3 视频播放控制细节
- **触摸控制**：支持水平滑动调节播放进度
- **进度同步**：实时更新进度条和时间显示
- **循环播放**：视频播放完毕后自动循环
- **滑动防抖**：避免在拖拽时触发其他手势事件

## 7. 媒体资源管理

### 7.1 图片资源
- 使用Glide库进行图片加载和缓存
- SquareImageView确保正方形显示
- 支持长按保存到本地相册

### 7.2 视频资源
- 使用VideoView进行播放
- 提供自定义播放控制器
- 支持长按保存视频到本地

### 7.3 网格布局
- 使用GridLayoutManager实现3列网格布局
- GridSpacingItemDecoration添加均匀间距
- 无过度滚动效果

## 8. 数据交互与网络通信

### 8.1 后端服务
- 使用Supabase作为后端服务
- SupabaseModule封装所有API调用
- 支持动态创建、获取、删除
- 支持评论添加、删除
- 支持点赞功能

### 8.2 文件存储
- 图片存储在`post-images`存储桶
- 视频同样存储在`post-images`存储桶
- 头像存储在`avatars`存储桶
- 文件名使用UUID确保唯一性

## 9. 用户交互功能

### 9.1 动态操作
- 长按动态可删除（需要确认对话框）
- 点赞功能（带震动反馈）
- 评论功能（支持评论回复和删除）

### 9.2 媒体操作
- 长按图片/视频可保存到本地
- 权限管理（Android 10以上无需存储权限）
- 振动反馈增强用户体验

### 9.3 界面交互
- 下拉刷新加载最新动态
- 平滑滚动到指定动态
- 键盘弹出时智能调整布局

## 10. 性能优化

### 10.1 内存优化
- 图片加载使用Glide内存管理
- 视频播放结束后及时释放资源
- Bitmap及时回收避免内存泄漏

### 10.2 网络优化
- 视频压缩减少上传流量（目标40MB以内）
- 图片压缩减少加载时间
- 智能缓存策略

### 10.3 UI优化
- RecyclerView复用机制
- 异步加载避免UI卡顿
- 进度指示器提升用户体验

这份技术文档全面介绍了ZhuDeApp中Post功能的所有相关组件、架构设计、实现逻辑和优化措施。整个系统采用现代化的Android开发架构，结合MVVM模式、协程、Data Binding等技术，实现了功能丰富且用户体验良好的动态发布和浏览功能。