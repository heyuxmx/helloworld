package com.heyu.zhudeapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.databinding.PostImageItemBinding

/**
 * 用于在动态详情或列表中显示图片网格的适配器。
 */
class PostImagesAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

    /**
     * ViewHolder, 包含一个对 post_image_item.xml 布局的绑定引用。
     */
    class ImageViewHolder(val binding: PostImageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = PostImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.image_placeholder) // 先显示一个柔和的灰色占位图
            .error(R.drawable.image_placeholder)       // 加载失败时也显示占位图
            .thumbnail(0.1f) // 先加载一个10%尺寸的模糊缩略图，然后再加载全图
            .into(holder.binding.postImageItemView)

        // 设置点击监听器，用于打开新的图片查看器页面
        holder.binding.postImageItemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                // 将被点击图片的 URL 作为参数传递给新的 Activity
                putExtra(ImageViewerActivity.IMAGE_URL, imageUrl)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
