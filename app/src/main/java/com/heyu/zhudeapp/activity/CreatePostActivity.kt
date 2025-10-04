package com.heyu.zhudeapp.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.adapter.SelectedImagesAdapter
import com.heyu.zhudeapp.databinding.ActivityCreatePostBinding
import com.heyu.zhudeapp.di.SupabaseModule
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    private val selectedImageUris = mutableListOf<Uri>()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        uris.let {
            selectedImageUris.addAll(it)
            selectedImagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.addImageButton.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.publishButton.setOnClickListener {
            publishPost()
        }
    }

    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter(selectedImageUris) { uri ->
            // Handle remove image
            val position = selectedImageUris.indexOf(uri)
            if (position != -1) {
                selectedImageUris.removeAt(position)
                selectedImagesAdapter.notifyItemRemoved(position)
            }
        }
        binding.selectedImagesRecyclerView.apply {
            adapter = selectedImagesAdapter
            layoutManager = LinearLayoutManager(this@CreatePostActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun publishPost() {
        val content = binding.contentEditText.text.toString()
        if (content.isBlank() && selectedImageUris.isEmpty()) {
            Toasty.warning(this, "内容不能为空").show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val imageUrls = mutableListOf<String>()
                if (selectedImageUris.isNotEmpty()) {
                    // 遍历所有选择的图片
                    for (uri in selectedImageUris) {
                        // 步骤1：压缩图片，将其变为轻量级的字节数组
                        val imageBytes = SupabaseModule.compressImage(this@CreatePostActivity, uri)

                        // 步骤2：为压缩后的图片（.jpg）生成一个唯一的文件名
                        val fileName = "${UUID.randomUUID()}.jpg"
                        
                        // 步骤3：上传压缩后的图片数据
                        val url = SupabaseModule.uploadPostImage(imageBytes, fileName)
                        imageUrls.add(url)
                    }
                }

                // 使用包含所有图片URL的列表创建动态
                SupabaseModule.createPost(content, imageUrls)
                Toasty.success(this@CreatePostActivity, "发布成功").show()
                finish()
            } catch (e: Exception) {
                Toasty.error(this@CreatePostActivity, "发布失败: ${e.message}").show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.publishButton.isEnabled = !isLoading
        binding.contentEditText.isEnabled = !isLoading
        binding.addImageButton.isEnabled = !isLoading
    }
}
