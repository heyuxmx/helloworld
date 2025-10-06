package com.heyu.zhudeapp.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.OnItemLongClickListener
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentSecondBinding
import com.heyu.zhudeapp.viewmodel.MainViewModel
import com.heyu.zhudeapp.viewmodel.PostViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class PostFragment : Fragment(), OnItemLongClickListener {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    // This ViewModel is for the posts list of this fragment
    private lateinit var viewModel: PostViewModel
    // This ViewModel is shared with MainActivity for navigation events
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(PostViewModel::class.java)

        setupRecyclerView()
        observeViewModel()
        setupFab()
        setupFragmentResultListener()
        setupDaysCounter()

        // Start listening for navigation events from the MainActivity
        observeNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Load posts every time the fragment becomes visible.
        loadPosts()
    }

    private fun observeNavigation() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.navigateToPost.collect { postId ->
                if (postId.isNotBlank()) {
                    // TODO: Replace this Toast with your actual navigation logic
                    // to open the detail screen for the given postId.
                    Toasty.info(requireContext(), "接收到通知跳转指令，目标动态ID: $postId", Toasty.LENGTH_LONG).show()

                    // Important: Consume the event after handling it to prevent re-navigation
                    // on configuration changes (e.g., screen rotation).
                    mainViewModel.onNavigationComplete()
                }
            }
        }
    }

    private fun setupDaysCounter() {
        val startDate = java.util.Calendar.getInstance().apply {
            set(2024, java.util.Calendar.DECEMBER, 2, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val diffInMillis = today.timeInMillis - startDate.timeInMillis
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
        binding.daysNumberTextView.text = days.toString()
    }

    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener(DeleteConfirmationDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val confirmed = bundle.getBoolean(DeleteConfirmationDialogFragment.BUNDLE_KEY_CONFIRMED)
            if (confirmed) {
                val postJson = bundle.getString(DeleteConfirmationDialogFragment.BUNDLE_KEY_POST)
                postJson?.let {
                    val post = Json.decodeFromString<Post>(it)
                    deletePost(post)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(emptyList(), this)
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.updatePosts(posts)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toasty.error(requireContext(), error, Toasty.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
