package com.heyu.zhudeapp.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.activity.CreatePostActivity
import com.heyu.zhudeapp.adapter.PostAdapter
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentSecondBinding
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.launch

/**
 * 应用的核心主屏幕，用于显示动态列表并提供发布入口。
 */
class SecondFragment : Fragment() {

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
        // 修正1: 明确指定空列表的类型为 List<Post>
        postAdapter = PostAdapter(emptyList<Post>())

        // 修正2: 使用 requireContext() 并直接设置属性
        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = postAdapter
    }

    /**
     * 从 Supabase 异步加载动态并更新到 UI
     */
    private fun loadPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val posts = SupabaseModule.getPosts()
                postAdapter.updatePosts(posts)
            } catch (e: Exception) {
                // 可以在这里添加错误提示，例如 Toast
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
