package com.heyu.zhudeapp.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.heyu.zhudeapp.adapter.CommentAdapter
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentCommentsBinding
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.di.UserManager
import com.heyu.zhudeapp.viewmodel.UserManagementViewModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class CommentsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var post: Post
    private val commentsList = mutableListOf<Comment>()
    private lateinit var commentAdapter: CommentAdapter
    private val userViewModel: UserManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            post = it.getParcelable("post")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel.fetchCurrentUser() // Fetch user profile
        setupRecyclerView()
        fetchComments()

        binding.sendButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postNewComment(commentText)
            }
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(commentsList) { comment ->
            onCommentLongClicked(comment)
        }
        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }
    }

    private fun onCommentLongClicked(comment: Comment) {
        // Only allow the author of the comment to delete it.
        if (comment.userId == UserManager.getCurrentUserId()) {
            showDeleteConfirmationDialog(comment)
        }
    }

    private fun fetchComments() {
        lifecycleScope.launch {
            try {
                val result = SupabaseModule.supabase.from("comments")
                    .select(Columns.raw("*, author:users(*)")) {
                        filter {
                            eq("post_id", post.id)
                        }
                        order("created_at", Order.ASCENDING)
                    }.decodeList<Comment>()

                commentAdapter.updateComments(result)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to fetch comments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postNewComment(text: String) {
        lifecycleScope.launch {
            val userId = UserManager.getCurrentUserId()
            val currentUser = userViewModel.currentUser.value

            if (currentUser == null) {
                Toast.makeText(requireContext(), "正在获取用户信息，请稍后重试", Toast.LENGTH_SHORT).show()
                userViewModel.fetchCurrentUser() // Re-fetch user if not available
                return@launch
            }

            try {
                val newCommentForDb = Comment(
                    postId = post.id,
                    content = text,
                    userId = userId
                )

                // Insert the comment into the database
                SupabaseModule.supabase.from("comments").insert(newCommentForDb)

                // Clear input and refresh the list from the server to get the complete data
                binding.commentInput.text.clear()
                fetchComments()

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除评论")
            .setMessage("您确定要删除这条评论吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteComment(comment)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch {
            try {
                SupabaseModule.supabase.from("comments").delete {
                    filter {
                        eq("id", comment.id)
                    }
                }
                // Refresh the comments list after deletion
                fetchComments()
                Toast.makeText(context, "评论已删除", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(post: Post): CommentsFragment {
            val fragment = CommentsFragment()
            val args = Bundle()
            args.putParcelable("post", post)
            fragment.arguments = args
            return fragment
        }
    }
}
