package com.heyu.zhudeapp.Fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.FragmentMineBinding
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.model.Users
import es.dmoral.toasty.Toasty
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.avatarImageView.setOnClickListener {
            if (binding.usernameEditText.isEnabled) {
                openImagePicker()
            } else {
                Toasty.info(requireContext(), "请先点击铅笔图标进入编辑模式", Toasty.LENGTH_SHORT).show()
            }
        }

        binding.editUsernameButton.setOnClickListener {
            setEditMode(true)
        }

        binding.saveProfileButton.setOnClickListener {
            saveUserProfile()
        }

        binding.switchUserButton.setOnClickListener {
            switchUser()
        }
    }

    private fun setEditMode(enabled: Boolean) {
        binding.usernameEditText.isEnabled = enabled
        if (enabled) {
            binding.usernameEditText.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.usernameEditText, InputMethodManager.SHOW_IMPLICIT)
            binding.saveProfileButton.visibility = View.VISIBLE
            binding.editUsernameButton.visibility = View.GONE
        } else {
            binding.usernameEditText.clearFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
            binding.saveProfileButton.visibility = View.GONE
            binding.editUsernameButton.visibility = View.VISIBLE
        }
    }

    private fun switchUser() {
        if (binding.usernameEditText.isEnabled) {
            setEditMode(false) // Exit edit mode before switching
        }
        val newUserId = if (Users.getActiveUserId(requireContext()) == Users.USER_ID_GAOBAO) Users.USER_ID_XUBABA else Users.USER_ID_GAOBAO
        Users.setActiveUserId(requireContext(), newUserId)
        loadUserProfile()
        Toasty.success(requireContext(), "已切换到 ${Users.getCurrentUsername(requireContext())}", Toasty.LENGTH_SHORT).show()
    }

    // --- REFACTORED: The final, correct implementation ---
    private fun loadUserProfile() {
        val username = Users.getCurrentUsername(requireContext())
        // This new function returns an `Any` type: a String URL if custom, or an Int resource ID if default
        val avatarResource = Users.getAvatarForDisplay(requireContext())

        binding.usernameEditText.setText(username)

        // Glide can load from many types, including String (URL) and Int (resource ID)
        Glide.with(this)
            .load(avatarResource) // This will now work for both cases
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder_error)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable cache to always show the latest avatar
            .skipMemoryCache(true)
            .into(binding.avatarImageView)

        setEditMode(false)
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImage.launch(intent)
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                selectedImageUri = it
                binding.avatarImageView.setImageURI(it)
            }
        }
    }

    private fun saveUserProfile() {
        binding.saveProfileButton.isEnabled = false
        val username = binding.usernameEditText.text.toString().trim()

        lifecycleScope.launch {
            try {
                // Step 1: If a new image was picked, upload it and get the URL.
                val newAvatarUrl = selectedImageUri?.let {
                    withContext(Dispatchers.IO) {
                        uploadImageToSupabase(it)
                    }
                }

                // Step 2: Save data using our Users object on the main thread.
                withContext(Dispatchers.Main) {
                    Users.saveUsername(requireContext(), username)
                    newAvatarUrl?.let {
                        Users.saveAvatarUrl(requireContext(), it)
                    }

                    selectedImageUri = null // Reset after a successful save
                    Toasty.success(requireContext(), "保存成功", Toasty.LENGTH_SHORT).show()
                    loadUserProfile() // Reload profile to reflect changes immediately
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toasty.error(requireContext(), "保存失败: ${e.message}", Toasty.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.saveProfileButton.isEnabled = true
                }
            }
        }
    }

    private suspend fun uploadImageToSupabase(uri: Uri): String {
        val context = requireContext().applicationContext
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read image file.")

        val filePath = "avatar_${Users.getActiveUserId(context)}.jpg"

        SupabaseModule.supabase.storage.from("avatars").upload(filePath, bytes, upsert = true)

        return SupabaseModule.supabase.storage.from("avatars").publicUrl(filePath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
