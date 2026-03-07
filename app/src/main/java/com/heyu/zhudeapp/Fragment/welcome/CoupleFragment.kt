package com.heyu.zhudeapp.Fragment.welcome

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
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CoupleFragment : Fragment() {

    private var _binding: FragmentCoupleBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val USER1 = "12345"
        private const val USER2 = "67890"
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
        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 并行获取用户信息和动态列表
                val users = HeyuModule.getUsers()
                val posts = HeyuModule.getPosts()

                if (_binding == null) return@launch

                val user1 = users.find { it.id == USER1 }
                val user2 = users.find { it.id == USER2 }

                // 资料卡 - 用户1
                binding.nameUser1.text = user1?.username ?: "高猪猪"
                loadAvatar(user1?.avatarUrl, binding.avatarUser1)
                loadAvatar(user1?.avatarUrl, binding.contribAvatar1)

                // 资料卡 - 用户2
                binding.nameUser2.text = user2?.username ?: "徐大王"
                loadAvatar(user2?.avatarUrl, binding.avatarUser2)
                loadAvatar(user2?.avatarUrl, binding.contribAvatar2)

                // 统计
                val totalPosts = posts.size
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

                binding.statTotalPosts.text = totalPosts.toString()
                binding.statTotalImages.text = totalImages.toString()
                binding.statTotalVideos.text = totalVideos.toString()

                binding.contribName1.text = user1?.username ?: "高猪猪"
                binding.contribCount1.text = "${user1Posts} 条动态"
                binding.contribName2.text = user2?.username ?: "徐大王"
                binding.contribCount2.text = "${user2Posts} 条动态"

            } catch (_: Exception) {
                // 网络失败时静默处理，保留默认 UI
            }
        }
    }

    private fun loadAvatar(url: String?, imageView: android.widget.ImageView) {
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
