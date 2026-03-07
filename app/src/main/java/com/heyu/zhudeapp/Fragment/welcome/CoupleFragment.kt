package com.heyu.zhudeapp.Fragment.welcome

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.FragmentCoupleBinding
import com.heyu.zhudeapp.di.HeyuModule
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CoupleFragment : Fragment() {

    private var _binding: FragmentCoupleBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val USER1 = "12345"
        private const val USER2 = "67890"
        private const val PREFS_NAME = "couple_cache"
        private const val KEY_DATA = "cached_data"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoupleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.daysTogetherText.text = "已相恋 ${calcDays()} 天"

        // 先加载本地缓存，立刻显示
        loadFromCache()
        // 再从网络刷新
        refreshFromNetwork()
    }

    private fun loadFromCache() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DATA, null) ?: return
        try {
            val data = JSONObject(json)
            applyData(
                name1 = data.optString("name1", "高猪猪"),
                avatar1 = data.optString("avatar1", ""),
                name2 = data.optString("name2", "徐大王"),
                avatar2 = data.optString("avatar2", ""),
                totalPosts = data.optInt("totalPosts", 0),
                totalImages = data.optInt("totalImages", 0),
                totalVideos = data.optInt("totalVideos", 0),
                user1Posts = data.optInt("user1Posts", 0),
                user2Posts = data.optInt("user2Posts", 0)
            )
        } catch (_: Exception) {
            // 缓存解析失败，等待网络数据
        }
    }

    private fun saveToCache(
        name1: String, avatar1: String?,
        name2: String, avatar2: String?,
        totalPosts: Int, totalImages: Int, totalVideos: Int,
        user1Posts: Int, user2Posts: Int
    ) {
        val json = JSONObject().apply {
            put("name1", name1)
            put("avatar1", avatar1 ?: "")
            put("name2", name2)
            put("avatar2", avatar2 ?: "")
            put("totalPosts", totalPosts)
            put("totalImages", totalImages)
            put("totalVideos", totalVideos)
            put("user1Posts", user1Posts)
            put("user2Posts", user2Posts)
        }
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATA, json.toString()).apply()
    }

    private fun refreshFromNetwork() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val users = HeyuModule.getUsers()
                val posts = HeyuModule.getPosts()

                if (_binding == null) return@launch

                val user1 = users.find { it.id == USER1 }
                val user2 = users.find { it.id == USER2 }

                val name1 = user1?.username ?: "高猪猪"
                val name2 = user2?.username ?: "徐大王"
                val avatar1 = user1?.avatarUrl
                val avatar2 = user2?.avatarUrl

                var totalImages = 0
                var totalVideos = 0
                var user1Posts = 0
                var user2Posts = 0

                for (post in posts) {
                    for (url in post.imageUrls) {
                        if (url.contains(".mp4", ignoreCase = true)) {
                            totalVideos++
                        } else {
                            totalImages++
                        }
                    }
                    when (post.userId) {
                        USER1 -> user1Posts++
                        USER2 -> user2Posts++
                    }
                }

                applyData(name1, avatar1, name2, avatar2,
                    posts.size, totalImages, totalVideos, user1Posts, user2Posts)

                saveToCache(name1, avatar1, name2, avatar2,
                    posts.size, totalImages, totalVideos, user1Posts, user2Posts)

            } catch (_: Exception) {
                // 网络失败，保留缓存数据
            }
        }
    }

    private fun applyData(
        name1: String, avatar1: String?,
        name2: String, avatar2: String?,
        totalPosts: Int, totalImages: Int, totalVideos: Int,
        user1Posts: Int, user2Posts: Int
    ) {
        if (_binding == null) return

        binding.nameUser1.text = name1
        loadAvatar(avatar1, binding.avatarUser1)
        loadAvatar(avatar1, binding.contribAvatar1)

        binding.nameUser2.text = name2
        loadAvatar(avatar2, binding.avatarUser2)
        loadAvatar(avatar2, binding.contribAvatar2)

        binding.statTotalPosts.text = totalPosts.toString()
        binding.statTotalImages.text = totalImages.toString()
        binding.statTotalVideos.text = totalVideos.toString()

        binding.contribName1.text = name1
        binding.contribCount1.text = "${user1Posts} 条动态"
        binding.contribName2.text = name2
        binding.contribCount2.text = "${user2Posts} 条动态"
    }

    private fun loadAvatar(url: String?, imageView: android.widget.ImageView) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_default_avatar)
            return
        }
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    private fun calcDays(): Long {
        val startDate = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(today.timeInMillis - startDate.timeInMillis) + 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
