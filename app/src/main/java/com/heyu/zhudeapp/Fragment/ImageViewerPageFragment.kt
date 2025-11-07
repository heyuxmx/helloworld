package com.heyu.zhudeapp.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.FragmentImageViewerPageBinding

class ImageViewerPageFragment : Fragment() {

    private var _binding: FragmentImageViewerPageBinding? = null
    private val binding get() = _binding!!

    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUrl = it.getString(ARG_IMAGE_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageViewerPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .into(binding.photoView)
        }
        // Set a listener to finish the activity when the photo is tapped.
        binding.photoView.setOnPhotoTapListener { _, _, _ ->
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        @JvmStatic
        fun newInstance(imageUrl: String) =
            ImageViewerPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
    }
}