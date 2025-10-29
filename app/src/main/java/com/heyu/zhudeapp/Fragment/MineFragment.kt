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
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.FragmentMineBinding
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.di.UserManager
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

        loadUserProfileFromServer()

        binding.avatarImageView.setOnClickListener {
            openImagePicker()
        }

        binding.editUsernameButton.setOnClickListener {
            setEditMode(true)
        }

        binding.saveProfileButton.setOnClickListener {
            saveUserProfileToServer()
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

    private fun loadUserProfileFromServer() {
        lifecycleScope.launch {
            try {
                // UserManager correctly determines the user ID based on the app version
                val userId = UserManager.getCurrentUserId()
                val userProfile = withContext(Dispatchers.IO) {
                    SupabaseModule.getUserById(userId)
                }

                if (userProfile != null) {
                    binding.usernameEditText.setText(userProfile.username)
                    Glide.with(this@MineFragment)
                        .load(userProfile.avatarUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder_error)
                        .circleCrop()
                        .into(binding.avatarImageView)
                } else {
                    Toasty.error(requireContext(), "加载用户信息失败", Toasty.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toasty.error(requireContext(), "加载用户信息失败: ${e.message}", Toasty.LENGTH_LONG).show()
            }
        }
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
                Glide.with(this@MineFragment)
                    .load(it)
                    .circleCrop()
                    .into(binding.avatarImageView)
                // After picking an image, automatically enter edit mode
                setEditMode(true)
            }
        }
    }

    private fun saveUserProfileToServer() {
        binding.saveProfileButton.isEnabled = false
        val newUsername = binding.usernameEditText.text.toString().trim()

        lifecycleScope.launch {
            try {
                // Step 1: If a new image was picked, upload it and get the URL.
                val newAvatarUrl = selectedImageUri?.let {
                    withContext(Dispatchers.IO) {
                        uploadImageToSupabase(it)
                    }
                }

                // Step 2: Update the profile in the database.
                val userId = UserManager.getCurrentUserId()
                withContext(Dispatchers.IO) {
                    SupabaseModule.updateUserProfile(userId, newUsername, newAvatarUrl)
                }

                // Step 3: Reload the profile from the server to reflect changes.
                withContext(Dispatchers.Main) {
                    selectedImageUri = null // Reset after a successful save
                    Toasty.success(requireContext(), "保存成功", Toasty.LENGTH_SHORT).show()
                    loadUserProfileFromServer() // Reload profile to reflect changes immediately
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
        val bytes = SupabaseModule.compressImage(context, uri)

        val filePath = "avatar_${UserManager.getCurrentUserId()}_${System.currentTimeMillis()}.jpg"

        SupabaseModule.supabase.storage.from("avatars").upload(filePath, bytes, upsert = true)

        return SupabaseModule.supabase.storage.from("avatars").publicUrl(filePath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
