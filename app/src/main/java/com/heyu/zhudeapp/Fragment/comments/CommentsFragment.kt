package com.heyu.zhudeapp.Fragment.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.adapter.CommentAdapter
import com.heyu.zhudeapp.databinding.FragmentCommentsBinding
import com.heyu.zhudeapp.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
            // The post object is now successfully passed here
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
        fetchComments() // Fetch comments from Supabase when the view is created

        binding.sendButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postNewComment(commentText) // Post the new comment to Supabase
            }
        }
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
                val result = SupabaseClient.client.from("comments")
                    .select {
                        filter {
                            eq("post_id", post.id)
                        }
                        order("created_at", Order.ASCENDING)
                    }.decodeList<Comment>()

                commentsList.clear()
                commentsList.addAll(result)
                commentAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to fetch comments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postNewComment(text: String) {
        lifecycleScope.launch {
            try {
                // Here you should get the actual user's name, using a placeholder for now
                val userName = "New User" // Replace with actual user name logic

                val newComment = Comment(
                    postId = post.id,
                    userName = userName,
                    text = text
                )

                // Insert the new comment and decode the result to get the ID and created_at
                val result = SupabaseClient.client.from("comments")
                    .insert(newComment) { select() } // Use select() to get the inserted row back
                    .decodeSingle<Comment>()

                // Add to list and update UI
                commentsList.add(result)
                commentAdapter.notifyItemInserted(commentsList.size - 1)
                binding.commentsRecyclerView.scrollToPosition(commentsList.size - 1)
                binding.commentInput.text.clear() // Clear input field

            } catch (e: Exception) {
                 Toast.makeText(context, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
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
