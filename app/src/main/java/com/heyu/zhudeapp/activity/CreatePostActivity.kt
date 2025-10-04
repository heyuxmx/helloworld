package com.heyu.zhudeapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.heyu.zhudeapp.databinding.ActivityCreatePostBinding
import com.heyu.zhudeapp.di.SupabaseModule
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

class CreatePostActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var binding: ActivityCreatePostBinding
    private var selectedImageUri: Uri? = null

    // ActivityResultLauncher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // For now, let's just update the add_image_button to show the selected image as a preview
            binding.addImageButton.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 1. Add Image Button
        binding.addImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 2. Publish Button
        binding.publishButton.setOnClickListener {
            publishPost()
        }
    }

    private fun publishPost() {
        val content = binding.contentEditText.text.toString()

        if (content.isBlank() && selectedImageUri == null) {
            Toasty.warning(this, "内容不能为空！").show()
            return
        }

        binding.publishButton.isEnabled = false
        Toasty.info(this, "发布中").show()

        lifecycleScope.launch {
            try {
                var imageUrl: String? = null
                // If an image was selected, upload it first
                selectedImageUri?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    val imageBytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (imageBytes != null) {
                        val fileExtension = getFileExtension(uri)
                        imageUrl = SupabaseModule.uploadPostImage(imageBytes, fileExtension)
                    }
                }

                // Create the post with the content and the (optional) uploaded image URL
                SupabaseModule.createPost(content, imageUrl)

                Toasty.success(this@CreatePostActivity, "Published successfully!").show()
                finish() // Close the activity after successful publishing

            } catch (e: Exception) {
                Toasty.error(this@CreatePostActivity, "Failed to publish: ${e.message}").show()
                binding.publishButton.isEnabled = true // Re-enable the button on failure
            }
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
    }
}
