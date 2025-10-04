package com.heyu.zhudeapp.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.OnItemLongClickListener
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentSecondBinding
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.launch

/**
 * 应用的核心主屏幕，用于显示动态列表并提供发布入口。
 */
// Implement the long click listener interface.
class SecondFragment : Fragment(), OnItemLongClickListener {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

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

        // 设置列表和“+”号按钮的点击事件
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        // 当用户从发布页返回时，自动加载/刷新动态列表
        loadPosts()
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            val intent = Intent(requireContext(), CreatePostActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 设置 RecyclerView，此函数已修正所有编译错误。
     */
    private fun setupRecyclerView() {
        // Pass 'this' as the listener to the adapter.
        postAdapter = PostAdapter(emptyList<Post>(), this)

        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = postAdapter
    }

    /**
     * This method is called when a post is long-clicked.
     */
    override fun onItemLongClick(post: Post) {
        // Show a confirmation dialog.
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                // If confirmed, proceed with deletion.
                deletePost(post)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * From Supabase 异步加载动态并更新到 UI
     */
    private fun loadPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val posts = SupabaseModule.getPosts()
                // 将获取到的动态列表反转，以实现倒序展示
                postAdapter.updatePosts(posts.reversed())
            } catch (e: Exception) {
                // 可以在这里添加错误提示，例如 Toast
            }
        }
    }

    /**
     * Deletes a post from Supabase and refreshes the list.
     */
    private fun deletePost(post: Post) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseModule.deletePost(post)
                // After deletion, reload the posts to update the UI.
                loadPosts()
            } catch (e: Exception) {
                // You could show a toast here to indicate the failure.
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
