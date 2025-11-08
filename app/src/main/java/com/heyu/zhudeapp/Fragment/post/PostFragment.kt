package com.heyu.zhudeapp.Fragment.post

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.OnCommentLongClickListener
import com.heyu.zhudeapp.adapter.OnImageSaveListener
import com.heyu.zhudeapp.adapter.OnItemLongClickListener
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentPostBinding
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.viewmodel.MainViewModel
import com.heyu.zhudeapp.viewmodel.PostViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PostFragment : Fragment(), OnItemLongClickListener, OnImageSaveListener,
    OnCommentLongClickListener {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PostViewModel
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var postAdapter: PostAdapter

    private var imageUrlToSave: String? = null
    // Variable to hold the post ID from a notification that needs to be scrolled to.
    private var pendingPostIdToScroll: String? = null


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imageUrlToSave?.let { saveImageToGallery(it) }
        } else {
            Toast.makeText(requireContext(), "保存图片需要存储权限", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(PostViewModel::class.java)

        setupRecyclerView()
        setupSwipeToRefresh()
        observeViewModel()
        setupFab()
        setupFragmentResultListener()
        setupDaysCounter()
        observeNavigation()

        // Trigger the initial load of posts.
        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        // The initial load is now handled in onViewCreated.
        // Additional onResume logic can be added here if needed.
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.navigateToPost.collect { postId ->
                if (postId.isNotBlank()) {
                    val postIndex = postAdapter.getPostIndex(postId)

                    if (postIndex != -1) {
                        // Post is already in the list, just scroll to it.
                        binding.postsRecyclerView.smoothScrollToPosition(postIndex)
                    } else {
                        // Post not found. Trigger a refresh and store the ID to scroll to later.
                        pendingPostIdToScroll = postId
                        binding.swipeRefreshLayout.isRefreshing = true
                        loadPosts()
                        Toasty.info(requireContext(), "正在加载新动态...", Toast.LENGTH_SHORT).show()
                    }
                    // Consume the event.
                    mainViewModel.onNavigationComplete()
                }
            }
        }
    }

    private fun setupDaysCounter() {
        val startDate = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = today.timeInMillis - startDate.timeInMillis
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
        binding.daysNumberTextView.text = days.toString()
    }

    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener(DeleteConfirmationDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val confirmed = bundle.getBoolean(DeleteConfirmationDialogFragment.BUNDLE_KEY_CONFIRMED)
            if (confirmed) {
                val postJson = bundle.getString(DeleteConfirmationDialogFragment.BUNDLE_KEY_POST)
                postJson?.let {
                    val post = Json.Default.decodeFromString<Post>(it)
                    deletePost(post)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = UserManager.getCurrentUserId()
        postAdapter = PostAdapter(
            posts = emptyList(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            currentUserId = currentUserId,
            onItemLongClickListener = this,
            onImageSaveListener = this,
            onCommentLongClickListener = this
        )
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            recycledViewPool.setMaxRecycledViews(0, 0)
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPosts()
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.updatePosts(posts)
            if (_binding != null) {
                binding.swipeRefreshLayout.isRefreshing = false
            }

            // After the new list is loaded, check if we need to scroll to a specific post.
            pendingPostIdToScroll?.let { postId ->
                val postIndex = postAdapter.getPostIndex(postId)
                if (postIndex != -1) {
                    binding.postsRecyclerView.smoothScrollToPosition(postIndex)
                }
                // Reset the pending ID after attempting to scroll.
                pendingPostIdToScroll = null
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toasty.error(requireContext(), error, Toasty.LENGTH_LONG).show()
            if (_binding != null) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            startActivity(Intent(requireContext(), CreatePostActivity::class.java))
        }
    }

    private fun loadPosts() {
        viewModel.fetchPosts()
    }

    override fun onItemLongClick(post: Post) {
        val dialog = DeleteConfirmationDialogFragment.newInstance(post)
        dialog.show(childFragmentManager, "DeleteConfirmationDialog")
    }

    override fun onCommentLongClick(post: Post, comment: Comment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除评论")
            .setMessage("您确定要删除这条评论吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deleteComment(comment)
            }
            .show()
    }

    override fun onImageSave(imageUrl: String) {
        this.imageUrlToSave = imageUrl

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery(imageUrl)
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveImageToGallery(imageUrl)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            try {
                viewModel.deletePost(post)
                Toasty.success(requireContext(), "删除成功!", Toasty.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toasty.error(requireContext(), "删除失败: ${e.message}", Toasty.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch {
            try {
                viewModel.deleteComment(comment)
                Toasty.success(requireContext(), "评论已删除", Toasty.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toasty.error(requireContext(), "删除失败: ${e.message}", Toasty.LENGTH_LONG).show()
            }
        }
    }

    private fun saveImageToGallery(imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()

                val savedUri = saveBitmapToMediaStore(bitmap, "Image_${System.currentTimeMillis()}.jpg")

                withContext(Dispatchers.Main) {
                    if (savedUri != null) {
                        Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "保存失败: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, displayName: String): Uri? {
        val context = requireContext()
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(imageCollection, contentValues)

        uri?.let { savedUri ->
            try {
                resolver.openOutputStream(savedUri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException("Failed to save bitmap.")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(savedUri, contentValues, null, null)
                }
                return@let savedUri
            } catch (e: Exception) {
                resolver.delete(savedUri, null, null)
                throw e
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.postsRecyclerView.adapter = null
        _binding = null
    }
}
