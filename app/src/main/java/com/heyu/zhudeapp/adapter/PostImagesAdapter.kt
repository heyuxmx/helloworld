package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
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
    }

    override fun getItemCount(): Int = imageUrls.size
}
