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
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.util.VideoUtils
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
                val imageUrls = uploadImages()
                updateDialog("正在保存动态...", 95)
                SupabaseModule.createPost(content, imageUrls, userId)
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

    private suspend fun uploadImages(): List<String> {
        val resultUrls = mutableListOf<String>()
        val total = selectedImageUris.size
        
        for ((index, uri) in selectedImageUris.withIndex()) {
            val countInfo = "(${index + 1}/$total)"
            val mimeType = contentResolver.getType(uri)
            val isVideo = mimeType?.startsWith("video/") == true
            
            var processedUri = uri
            if (isVideo) {
                // 压缩阶段：文字修正为“视频压缩中”
                updateDialog("视频压缩中 $countInfo...", 0)
                processedUri = VideoUtils.compressVideoIfNeeded(this, uri) { progress ->
                    updateDialog("视频压缩中 $countInfo...", progress)
                }
            }

            // 上传阶段：文字修正为“正在上传”
            updateDialog("正在上传 $countInfo...", 30) // 此时由于无法获取上传精确进度，显示 30% 基准
            
            val fileBytes = withContext(Dispatchers.IO) {
                if (isVideo) {
                    VideoUtils.uriToByteArrayWithLimit(this@CreatePostActivity, processedUri)
                } else {
                    SupabaseModule.compressImage(this@CreatePostActivity, uri)
                }
            }

            updateDialog("正在上传 $countInfo...", 70) // 数据读取完成，模拟进度到 70%

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
