package com.heyu.zhudeapp.activity

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.heyu.zhudeapp.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    companion object {
        const val EXTRA_IMAGE_URL = "image_url"
        // THIS IS THE KEY. It MUST match the one in MainActivity.
        const val EXTRA_CHANGE_AVATAR_REQUEST = "EXTRA_CHANGE_AVATAR_REQUEST"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)

        binding.progressBar.visibility = View.VISIBLE
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        binding.progressBar.visibility = View.GONE
                        // Optionally, show an error message or a placeholder
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        binding.progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.photoView)
        } else {
            binding.progressBar.visibility = View.GONE
            // Handle the case where the URL is null
        }

        binding.btnChangeAvatar.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_CHANGE_AVATAR_REQUEST, true)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
