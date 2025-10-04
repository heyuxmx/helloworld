package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.Post

class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    /**
     * ViewHolder 负责持有列表项的视图，避免每次都通过 findViewById 查找。
     */
    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentTextView: TextView = itemView.findViewById(R.id.post_content_text)
        val postImageView: ImageView = itemView.findViewById(R.id.post_image_view)
    }

    /**
     * 当 RecyclerView 需要一个新的 ViewHolder 时调用，用于创建列表项的视图。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view)
    }

    /**
     * 当 RecyclerView 需要将数据绑定到 ViewHolder 上时调用。
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // 1. 绑定动态的文本内容
        holder.contentTextView.text = post.content

        // 2. 检查是否有图片
        if (post.imageUrls.isNotEmpty()) {
            holder.postImageView.visibility = View.VISIBLE
            // 使用 Glide 加载第一张图片
            Glide.with(holder.itemView.context)
                .load(post.imageUrls[0])
                .into(holder.postImageView)
        } else {
            // 如果没有图片，确保 ImageView 是隐藏的
            holder.postImageView.visibility = View.GONE
        }
    }

    /**
     * 返回数据列表的总数。
     */
    override fun getItemCount(): Int {
        return posts.size
    }

    /**
     * 用于在获取到新数据后，更新适配器中的数据并刷新列表。
     */
    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged() // 通知 RecyclerView 数据已变更，需要重绘
    }
}
