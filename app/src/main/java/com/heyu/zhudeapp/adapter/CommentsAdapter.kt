package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post

// The constructor is updated to receive the post and the long-click listener.
class CommentsAdapter(
    private val mutableComments: MutableList<Comment>,
    private val post: Post,
    private val onCommentLongClickListener: OnCommentLongClickListener
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = mutableComments[position]
        holder.bind(comment)

        // Set the long-click listener on the entire comment item view.
        holder.itemView.setOnLongClickListener {
            // When a long click occurs, invoke the callback with the post and the specific comment.
            onCommentLongClickListener.onCommentLongClick(post, comment)
            true // Return true to indicate that the event has been consumed.
        }
    }

    override fun getItemCount(): Int = mutableComments.size

    /**
     * Adds a new comment to the list and notifies the adapter.
     */
    fun addComment(comment: Comment) {
        mutableComments.add(comment)
        notifyItemInserted(mutableComments.size - 1)
    }

    /**
     * Removes a comment from the list and notifies the adapter.
     * This will be called from your Activity/Fragment after the user confirms the deletion.
     */
    fun removeComment(comment: Comment) {
        val position = mutableComments.indexOf(comment)
        if (position != -1) {
            mutableComments.removeAt(position)
            notifyItemRemoved(position)
        }
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
            // Using .content is more readable than component3()
            commentContentTextView.text = comment.content
        }
    }
}
