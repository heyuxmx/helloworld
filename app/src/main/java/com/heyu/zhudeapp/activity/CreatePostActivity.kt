package com.heyu.zhudeapp.activity

import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.adapter.SelectedImagesAdapter
import com.heyu.zhudeapp.databinding.ActivityCreatePostBinding
import com.heyu.zhudeapp.di.HeyuModule
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.util.VideoUtils
import com.heyu.zhudeapp.util.VideoCacheManager
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    private val selectedImageUris = mutableListOf<Uri>()

    private var progressDialog: AlertDialog? = null
    private var dialogProgressBar: LinearProgressIndicator? = null
    private var dialogStatusText: TextView? = null
    private var dialogPercentageText: TextView? = null

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) {
            val filteredUris = uris.filter { uri ->
                val mimeType = contentResolver.getType(uri)
                val isVideo = mimeType?.startsWith("video/") == true
                if (isVideo && VideoUtils.isVideoTooLong(this@CreatePostActivity, uri)) {
                    Toasty.warning(this@CreatePostActivity, "视频时长过长，已自动过滤").show()
                    false
                } else {
                    true
                }
            }
            selectedImageUris.addAll(filteredUris)
            selectedImagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        setupClickListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter(selectedImageUris) { uri ->
            selectedImageUris.remove(uri)
            selectedImagesAdapter.notifyDataSetChanged()
        }
        binding.selectedImagesRecyclerView.apply {
            adapter = selectedImagesAdapter
            layoutManager = LinearLayoutManager(this@CreatePostActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        binding.addImageButton.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        binding.publishButton.setOnClickListener {
            publishPost()
        }
    }

    private fun showProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_upload_progress, null)
        dialogProgressBar = dialogView.findViewById(R.id.dialog_progress_bar)
        dialogStatusText = dialogView.findViewById(R.id.dialog_status_text)
        dialogPercentageText = dialogView.findViewById(R.id.dialog_percentage_text)

        progressDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .show()
    }

    private fun updateDialog(status: String, progress: Int) {
        runOnUiThread {
            dialogStatusText?.text = status
            dialogProgressBar?.progress = progress
            dialogPercentageText?.text = "$progress%"
        }
    }

    private fun publishPost() {
        val content = binding.contentEditText.text.toString().trim()
        if (content.isBlank() && selectedImageUris.isEmpty()) {
            Toasty.warning(this, getString(R.string.post_content_cannot_be_empty)).show()
            return
        }

        val userId = UserManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            showProgressDialog()
            try {
                // 原视频本地库同步逻辑 + 并发上传
                val imageUrls = uploadImages()
                updateDialog("正在保存动态...", 95)
                HeyuModule.createPost(content, imageUrls, userId)
                progressDialog?.dismiss()
                Toasty.success(this@CreatePostActivity, getString(R.string.publish_success)).show()
                sendSmsNotification()
                finish()
            } catch (e: Exception) {
                progressDialog?.dismiss()
                Toasty.error(this@CreatePostActivity, "发布失败: ${e.localizedMessage}").show()
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private suspend fun uploadImages(): List<String> {
        val total = selectedImageUris.size
        val progressMap = ConcurrentHashMap<Int, Int>()
        
        return coroutineScope {
            selectedImageUris.mapIndexed { index, uri ->
                async(Dispatchers.IO) {
                    val mimeType = contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video/") == true
                    
                    // --- 步骤 1: 压缩/处理 ---
                    var processedUri = uri
                    if (isVideo) {
                        processedUri = VideoUtils.compressVideoIfNeeded(this@CreatePostActivity, uri) { p ->
                            // 压缩进度占 40%
                            progressMap[index] = (p * 0.4).toInt()
                            updateGlobalProgress(progressMap, total)
                        }
                    } else {
                        // 图片直接算压缩完成 10%
                        progressMap[index] = 10
                        updateGlobalProgress(progressMap, total)
                    }

                    // --- 步骤 2: 读取字节流 ---
                    val fileBytes = if (isVideo) {
                        VideoUtils.uriToByteArrayWithLimit(this@CreatePostActivity, processedUri)
                    } else {
                        HeyuModule.compressImage(this@CreatePostActivity, uri)
                    }
                    
                    // 读取完算 20% (如果是图片) 或基于压缩进度加 10%
                    progressMap[index] = if(isVideo) (progressMap[index] ?: 40) + 10 else 20
                    updateGlobalProgress(progressMap, total)

                    // --- 步骤 3: 上传 ---
                    val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
                    val url = if (isVideo) {
                        HeyuModule.uploadPostVideo(fileBytes, fileName)
                    } else {
                        HeyuModule.uploadPostImage(fileBytes, fileName)
                    }
                    
                    // --- 步骤 4: 归档本地库 (仅视频) ---
                    if (isVideo) {
                        VideoCacheManager.saveOriginalVideoToLibrary(this@CreatePostActivity, url, uri)
                    }

                    // 任务彻底完成 100%
                    progressMap[index] = 100
                    updateGlobalProgress(progressMap, total)
                    
                    url
                }
            }.awaitAll()
        }
    }

    private fun updateGlobalProgress(progressMap: Map<Int, Int>, total: Int) {
        val currentSum = progressMap.values.sum()
        val averageProgress = currentSum / total
        updateDialog("正在极速发布中...", averageProgress.coerceIn(0, 99))
    }

    private fun sendSmsNotification() {
        try {
            val otherUserPhoneNumber = UserManager.getOtherUserPhoneNumber()
            val currentUserName = UserManager.getCurrentUserName()
            if (otherUserPhoneNumber != null && currentUserName != null) {
                val smsManager: SmsManager = this.getSystemService(SmsManager::class.java)
                val message = "$currentUserName 发布新动态了，快滚进来看！"
                smsManager.sendTextMessage(otherUserPhoneNumber, null, message, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
