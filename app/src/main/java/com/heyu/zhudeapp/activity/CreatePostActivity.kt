package com.heyu.zhudeapp.activity

import android.app.Activity
import android.os.Bundle
import com.heyu.zhudeapp.databinding.ActivityCreatePostBinding

class CreatePostActivity : Activity() {
    private lateinit var binding: ActivityCreatePostBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}
