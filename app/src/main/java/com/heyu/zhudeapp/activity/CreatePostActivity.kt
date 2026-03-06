package com.heyu.zhudeapp.activity

import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.adapter.SelectedImagesAdapter
import com.heyu.zhudeapp.databinding.ActivityCreatePostBinding
import com.heyu.zhudeapp.di.HeyuModule
import com.heyu.zhudeapp.di.UploadManager
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.util.ThemeManager
import com.heyu.zhudeapp.util.VideoUtils
import com.heyu.zhudeapp.util.VideoCacheManager
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    private val selectedImageUris = mutableListOf<Uri>()

    private var progressDialog: AlertDialog? = null
    private var dialogStatusText: TextView? = null

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
        ThemeManager.applyTheme(this)
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
        dialogStatusText = dialogView.findViewById(R.id.dialog_status_text)
        progressDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .show()
    }

    private fun updateStatus(status: String) {
        runOnUiThread { dialogStatusText?.text = status }
    }

    private fun publishPost() {
        val content = binding.contentEditText.text.toString().trim()
        if (content.isBlank() && selectedImageUris.isEmpty()) {
            Toasty.warning(this, getString(R.string.post_content_cannot_be_empty)).show()
            return
        }

        val userId = UserManager.getCurrentUserId() ?: return

        // 含视频：交给 UploadManager 后台上传，立即返回
        val hasVideo = selectedImageUris.any { uri ->
            contentResolver.getType(uri)?.startsWith("video/") == true
        }
        if (hasVideo) {
            UploadManager.enqueue(this, content, selectedImageUris.toList(), userId)
            sendSmsNotification()
            finish()
            return
        }

        // 纯图片：保持原有弹窗上传流程
        lifecycleScope.launch {
            showProgressDialog()
            try {
                val imageUrls = uploadImages()
                updateStatus("正在保存动态...")
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
        val uris = selectedImageUris.toList()
        if (uris.isEmpty()) return emptyList()

        val total = uris.size
        val completed = AtomicInteger(0)
        updateStatus("正在处理媒体文件...")

        return coroutineScope {
            uris.map { uri ->
                async(Dispatchers.IO) {
                    val mimeType = contentResolver.getType(uri)
                    val isVideo = mimeType?.startsWith("video/") == true

                    val fileBytes = if (isVideo) {
                        updateStatus("正在压缩视频...")
                        val compressed = VideoUtils.compressVideoIfNeeded(this@CreatePostActivity, uri) {}
                        VideoUtils.uriToByteArrayWithLimit(this@CreatePostActivity, compressed)
                    } else {
                        HeyuModule.compressImage(this@CreatePostActivity, uri)
                    }

                    val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
                    updateStatus("正在上传 (${completed.get() + 1}/$total)...")

                    val url = if (isVideo) {
                        HeyuModule.uploadPostVideo(fileBytes, fileName)
                    } else {
                        HeyuModule.uploadPostImage(fileBytes, fileName)
                    }

                    if (isVideo) {
                        VideoCacheManager.saveOriginalVideoToLibrary(this@CreatePostActivity, url, uri)
                    }

                    val done = completed.incrementAndGet()
                    updateStatus("已完成 $done/$total 个文件")
                    url
                }
            }.awaitAll()
        }
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
