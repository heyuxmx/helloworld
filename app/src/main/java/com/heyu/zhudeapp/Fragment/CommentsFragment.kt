package com.heyu.zhudeapp.Fragment

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.heyu.zhudeapp.adapter.CommentAdapter
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.FragmentCommentsBinding
import com.heyu.zhudeapp.di.SupabaseModule
import com.heyu.zhudeapp.di.UserManager
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
        setupRecyclerView()
        fetchComments()

        binding.sendButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postNewComment(commentText)
            }
        }

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val childView = binding.commentsRecyclerView.findChildViewUnder(e.x, e.y)
                if (childView != null) {
                    val position = binding.commentsRecyclerView.getChildAdapterPosition(childView)
                    if (position != RecyclerView.NO_POSITION) {
                        val comment = commentsList[position]
                        showDeleteConfirmationDialog(comment)
                    }
                }
            }
        })

        binding.commentsRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Let the gesture detector inspect the event.
                // If it consumes the event, we intercept.
                return gestureDetector.onTouchEvent(e)
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(commentsList)
        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
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
            
            try {
                val newComment = Comment(
                    postId = post.id,
                    content = text,
                    userId = userId
                )

                val result = SupabaseModule.supabase.from("comments")
                    .insert(newComment) { select(Columns.raw("*, author:users(*)")) }
                    .decodeSingle<Comment>()

                commentAdapter.addComment(result)
                binding.commentsRecyclerView.scrollToPosition(commentsList.size - 1)
                binding.commentInput.text.clear()

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

                val position = commentsList.indexOf(comment)
                if (position != -1) {
                    commentsList.removeAt(position)
                    commentAdapter.notifyItemRemoved(position)
                    commentAdapter.notifyItemRangeChanged(position, commentsList.size)
                }

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
