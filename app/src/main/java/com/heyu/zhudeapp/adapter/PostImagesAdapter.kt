package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            .into(holder.binding.postImageItemView)
    }

    override fun getItemCount(): Int = imageUrls.size
}
