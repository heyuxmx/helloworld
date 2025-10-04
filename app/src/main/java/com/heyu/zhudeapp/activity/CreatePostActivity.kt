package com.heyu.zhudeapp.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.R
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

    // Use the modern PickVisualMedia contract for a better user experience.
    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            selectedImagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter(selectedImageUris) { uri ->
            // A simpler way to remove the image from the list.
            selectedImageUris.remove(uri)
            selectedImagesAdapter.notifyDataSetChanged() // Notify for a full redraw, simpler for this case.
        }
        binding.selectedImagesRecyclerView.apply {
            adapter = selectedImagesAdapter
            layoutManager = LinearLayoutManager(this@CreatePostActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        binding.addImageButton.setOnClickListener {
            // Launch the modern photo picker.
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.publishButton.setOnClickListener {
            publishPost()
        }
    }

    private fun publishPost() {
        val content = binding.contentEditText.text.toString().trim()
        if (content.isBlank() && selectedImageUris.isEmpty()) {
            Toasty.warning(this, getString(R.string.post_content_cannot_be_empty)).show()
            return
        }

        lifecycleScope.launch {
            renderState(UiState.Loading)
            try {
                val imageUrls = uploadImages()
                SupabaseModule.createPost(content, imageUrls)
                renderState(UiState.Success)
            } catch (e: Exception) {
                renderState(UiState.Error(e.localizedMessage ?: getString(R.string.unknown_error)))
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val imageUrls = mutableListOf<String>()
        if (selectedImageUris.isNotEmpty()) {
            for (uri in selectedImageUris) {
                val imageBytes = SupabaseModule.compressImage(this@CreatePostActivity, uri)
                val fileName = "${UUID.randomUUID()}.jpg"
                val url = SupabaseModule.uploadPostImage(imageBytes, fileName)
                imageUrls.add(url)
            }
        }
        return imageUrls
    }

    private fun renderState(state: UiState) {
        when (state) {
            is UiState.Loading -> {
                binding.loadingIndicator.visibility = View.VISIBLE
                setInputsEnabled(false)
            }
            is UiState.Success -> {
                binding.loadingIndicator.visibility = View.GONE
                setInputsEnabled(true)
                Toasty.success(this, getString(R.string.publish_success)).show()
                finish()
            }
            is UiState.Error -> {
                binding.loadingIndicator.visibility = View.GONE
                setInputsEnabled(true)
                Toasty.error(this, getString(R.string.publish_failed, state.message)).show()
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.publishButton.isEnabled = enabled
        binding.contentEditText.isEnabled = enabled
        binding.addImageButton.isEnabled = enabled
    }

    // Sealed class to represent different UI states.
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}