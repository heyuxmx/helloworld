package com.heyu.zhudeapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.SelectedImageItemBinding

class SelectedImagesAdapter(
    private val images: MutableList<Uri>,
    private val onRemoveClicked: (Uri) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = SelectedImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        holder.bind(uri, onRemoveClicked)
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(private val binding: SelectedImageItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, onRemoveClicked: (Uri) -> Unit) {
            Glide.with(itemView.context)
                .load(uri)
                .into(binding.selectedImageView)

            binding.removeImageButton.setOnClickListener {
                onRemoveClicked(uri)
            }
        }
    }
}