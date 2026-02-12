package com.heyu.zhudeapp.Fragment.post

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.adapter.OnCommentInteractionListener
import com.heyu.zhudeapp.adapter.OnCommentLongClickListener
import com.heyu.zhudeapp.adapter.OnImageSaveListener
import com.heyu.zhudeapp.adapter.OnItemLongClickListener
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentPostBinding
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.viewmodel.PostViewModel
import com.heyu.zhudeapp.viewmodel.MainViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PostFragment : Fragment(), OnItemLongClickListener,
    OnImageSaveListener, OnCommentLongClickListener,
    OnCommentInteractionListener {

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: PostViewModel
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter
    private var imageUrlToSave: String? = null
    private var pendingPostIdToScroll: String? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var focusedPostId: Long? = null

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
        setupFocusCommentViewListeners()
        setupKeyboardListener()
        setupRecyclerViewTouchListener()

        // 启动即加载：ViewModel 会先给本地缓存，再后台同步
        loadPosts()

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                hideKeyboard()
            }
        }.also {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, it)
        }
    }

    private fun observeViewModel() {
        // 核心：这里的 LiveData 来自 Room 数据库
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (posts.isNullOrEmpty()) return@observe
            
            // 只要数据库里有，哪怕是断网状态，也会立刻显示
            postAdapter.updatePostsAndDrafts(posts, viewModel.commentDrafts.value?.mapValues { it.value ?: "" } ?: emptyMap())
            
            // 隐藏加载动画（无论是本地加载完还是网络同步完）
            if (_binding != null) {
                binding.swipeRefreshLayout.isRefreshing = false
            }

            // 预加载媒体文件到磁盘
            preFetchMedia(posts)

            pendingPostIdToScroll?.let { postId ->
                val postIndex = postAdapter.getPostIndex(postId)
                if (postIndex != -1) {
                    binding.postsRecyclerView.smoothScrollToPosition(postIndex)
                }
                pendingPostIdToScroll = null
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            // 网络报错不影响用户看本地帖子，只是提示一下同步失败
            Toasty.warning(requireContext(), error, Toasty.LENGTH_SHORT).show()
            if (_binding != null) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            viewModel.commentDrafts.collectLatest { drafts ->
                postAdapter.updatePostsAndDrafts(viewModel.posts.value ?: emptyList(), drafts?.mapValues { it.value ?: "" } ?: emptyMap())
            }
        }
    }

    /**
     * 极速预加载：利用 Glide 将图片 and 视频缩略图强行持久化到磁盘缓存
     */
    private fun preFetchMedia(posts: List<Post>) {
        posts.take(30).forEach { post ->
            post.author?.avatarUrl?.let {
                Glide.with(this).load(it).diskCacheStrategy(DiskCacheStrategy.ALL).preload()
            }
            post.imageUrls.forEach { url ->
                if (url.contains(".mp4", ignoreCase = true)) {
                    Glide.with(this).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload()
                } else {
                    Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = UserManager.getCurrentUserId()
        postAdapter = PostAdapter(
            posts = emptyList(),
            commentDrafts = emptyMap(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            currentUserId = currentUserId ?: "",
            onItemLongClickListener = this,
            onImageSaveListener = this,
            onCommentLongClickListener = this,
            onCommentInteractionListener = this
        )
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            // 优化：设置固定大小提高性能
            setHasFixedSize(true)
            // 优化：增加预取数量
            layoutManager?.let { (it as LinearLayoutManager).initialPrefetchItemCount = 4 }
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPosts()
        }
    }

    private fun loadPosts() {
        viewModel.fetchPosts()
    }

    // --- 以下为 UI 交互逻辑，保持原样 ---

    private fun setupKeyboardListener() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val newBottomMargin = if (imeVisible) (imeHeight - 3*navBarHeight).coerceAtLeast(0) else 0

            binding.focusCommentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = newBottomMargin
            }

            if (imeVisible) {
                binding.focusCommentContainer.visibility = View.VISIBLE
                onBackPressedCallback?.isEnabled = true
                binding.focusCommentContainer.post {
                    binding.postsRecyclerView.updatePadding(bottom = binding.focusCommentContainer.height)
                }
            } else {
                binding.focusCommentContainer.visibility = View.GONE
                onBackPressedCallback?.isEnabled = false
                focusedPostId = null
                binding.postsRecyclerView.updatePadding(bottom = 0)
            }

            val systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.swipeRefreshLayout.updatePadding(top = systemBarInsets.top, left = systemBarInsets.left, right = systemBarInsets.right)

            WindowInsetsCompat.Builder(windowInsets).setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 0)).build()
        }
    }

    private fun setupFocusCommentViewListeners() {
        binding.focusSendButton.setOnClickListener {
            val commentText = binding.focusCommentInput.text.toString().trim()
            val postId = focusedPostId
            if (commentText.isNotEmpty() && postId != null) {
                val currentUserId = UserManager.getCurrentUserId()
                viewModel.addComment(postId, commentText, currentUserId!!)
                viewModel.updateCommentDraft(postId, "")
                hideKeyboard()
            }
        }
        binding.focusCommentInput.addTextChangedListener { editable ->
            focusedPostId?.let { postId -> viewModel.updateCommentDraft(postId, editable.toString()) }
        }
    }

    private fun setupRecyclerViewTouchListener() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (binding.focusCommentContainer.visibility == View.VISIBLE) hideKeyboard()
                return super.onSingleTapUp(e)
            }
        })
        binding.postsRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onCommentDraftClicked(post: Post) {
        focusedPostId = post.id
        binding.focusCommentInput.setText(viewModel.commentDrafts.value[post.id] ?: "")
        binding.focusCommentInput.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.focusCommentInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.navigateToPost.collect { postId ->
                if (postId.isNotBlank()) {
                    val postIndex = postAdapter.getPostIndex(postId)
                    if (postIndex != -1) {
                        binding.postsRecyclerView.smoothScrollToPosition(postIndex)
                    } else {
                        pendingPostIdToScroll = postId
                        binding.swipeRefreshLayout.isRefreshing = true
                        loadPosts()
                        Toasty.info(requireContext(), "正在从服务器同步新动态...", Toast.LENGTH_SHORT).show()
                    }
                    mainViewModel.onNavigationComplete()
                }
            }
        }
    }

    private fun setupDaysCounter() {
        val startDate = Calendar.getInstance().apply { set(2024, Calendar.DECEMBER, 2, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
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

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            startActivity(Intent(requireContext(), CreatePostActivity::class.java))
        }
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
            .setPositiveButton("删除") { _, _ -> deleteComment(comment) }
            .show()
    }

    override fun onImageSave(imageUrl: String) {
        this.imageUrlToSave = imageUrl
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery(imageUrl)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun saveImageToGallery(imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = Glide.with(requireContext()).asBitmap().load(imageUrl).submit().get()
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ZhuDeApp")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        if (outputStream == null) throw IOException("Failed to get output stream.")
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) throw IOException("Failed to save bitmap.")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "图片已保存到相册", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "保存失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun deletePost(post: Post) {
        lifecycleScope.launch { viewModel.deletePost(post) }
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch { viewModel.deleteComment(comment) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
