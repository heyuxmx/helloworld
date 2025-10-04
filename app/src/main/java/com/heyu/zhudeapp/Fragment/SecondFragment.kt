package com.heyu.zhudeapp.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.OnItemLongClickListener
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentSecondBinding
import com.heyu.zhudeapp.viewmodel.PostViewModel
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SecondFragment : Fragment(), OnItemLongClickListener {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PostViewModel
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

        // Post loading is now handled in onResume to ensure the list is always fresh.
    }

    override fun onResume() {
        super.onResume()
        // Load posts every time the fragment becomes visible.
        // This ensures that new posts created in CreatePostActivity are displayed upon return.
        loadPosts()
    }

    private fun setupDaysCounter() {
        // Use Calendar for API 24+ compatibility and fix the start date.
        val startDate = java.util.Calendar.getInstance().apply {
            // Set to 2014-12-21. Note: Calendar months are 0-indexed (DECEMBER = 11).
            set(2024, java.util.Calendar.DECEMBER, 21, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val diffInMillis = today.timeInMillis - startDate.timeInMillis
        // Add 1 to include the start day in the count.
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
        binding.daysTextView.text = "今天是我们在一起的第${days}天啦"
    }

    private fun setupFragmentResultListener() {
        // Use the corrected keys from the companion object of DeleteConfirmationDialogFragment
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
        // Pass 'this' as the long click listener
        postAdapter = PostAdapter(emptyList(), this)
        // A standard LinearLayoutManager is used to display items from top to bottom.
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
            // Use Toasty for consistency
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
                // The observer will handle the UI update by reloading the posts
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
