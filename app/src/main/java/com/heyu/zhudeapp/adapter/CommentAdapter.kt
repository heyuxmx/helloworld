package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.Comment

class CommentAdapter(private var comments: MutableList<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    fun addComment(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.commentText)
        private val authorTextView: TextView = itemView.findViewById(R.id.commentUserName)

        fun bind(comment: Comment) {
            // Set the author's name with a colon, or "Anonymous:" if not available.
            authorTextView.text = "${comment.author?.username ?: "匿名用户"}："
            // Set the comment content
            contentTextView.text = comment.content
        }
    }
}
