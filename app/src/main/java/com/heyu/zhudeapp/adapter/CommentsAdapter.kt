package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.Comment

// Changed constructor to accept a List and internally convert it to a mutable one
class CommentsAdapter(comments: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    // Internal mutable list for managing comments
    private val mutableComments: MutableList<Comment> = comments.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        // Use the internal mutable list
        val comment = mutableComments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = mutableComments.size // Use the internal mutable list

    /**
     * Adds a new comment to the list and notifies the adapter.
     */
    fun addComment(comment: Comment) {
        mutableComments.add(comment)
        notifyItemInserted(mutableComments.size - 1)
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.commentUserName)
        private val commentContentTextView: TextView = itemView.findViewById(R.id.commentText)

        fun bind(comment: Comment) {
            val packageName = itemView.context.packageName
            // Set user name based on application ID suffix
            when {
                packageName.endsWith(".xiaoxu") -> userNameTextView.text = "徐大王:"
                packageName.endsWith(".xiaogao") -> userNameTextView.text = "高小妹:"
                else -> userNameTextView.text = "匿名用户:" // Default case
            }
            commentContentTextView.text = comment.component3()
        }
    }
}
