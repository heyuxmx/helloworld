package com.heyu.zhudeapp.Fragment.welcome

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.heyu.zhudeapp.databinding.FragmentWelcomeBinding
import com.heyu.zhudeapp.util.ThemeManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var animationToken = Any()
    private var days: Long = 0
    private var isTypewriterRunning = false

    companion object {
        // 静态变量，确保在整个应用生命周期内只播放一次打字效果
        private var hasPlayedTypewriter = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ThemeManager.isTech(requireContext())) {
            days = calcDays()
            binding.typewriterText.visibility = View.VISIBLE
            // 如果已经播放过，直接显示最终文本
            if (hasPlayedTypewriter) {
                binding.typewriterText.text = "${days}天啦！"
            } else {
                // 否则清空文本，等待 onBecomeVisible 启动动画
                binding.typewriterText.text = ""
            }
        }
    }

    /** Called by MainActivity when this fragment becomes visible */
    fun onBecomeVisible() {
        if (_binding == null || !ThemeManager.isTech(requireContext())) return
        // 如果已经播放过或者动画正在运行，不再启动
        if (!hasPlayedTypewriter && !isTypewriterRunning) {
            startTypewriter()
        }
        // If already played, text stays as-is
    }

    /** Called by MainActivity when this fragment becomes hidden */
    fun onBecomeHidden() {
        if (isTypewriterRunning) {
            // Animation running, cancel it and reset
            animationToken = Any()
            handler.removeCallbacksAndMessages(null)
            isTypewriterRunning = false
            _binding?.typewriterText?.text = ""
        } else if (!hasPlayedTypewriter) {
            // Animation not running but not played yet, just clear text
            _binding?.typewriterText?.text = ""
        }
        // If already played, leave the text visible
    }

    private fun startTypewriter() {
        // 防止重复启动
        if (isTypewriterRunning) return

        val token = Any()
        animationToken = token
        handler.removeCallbacksAndMessages(null)
        val tv = _binding?.typewriterText ?: return
        tv.text = ""

        isTypewriterRunning = true

        val line1 = "今天是我们在一起的"
        val line2 = "${days}天啦！"
        var time = 500L

        fun post(delay: Long, action: () -> Unit) {
            handler.postDelayed({
                if (animationToken === token && _binding != null) action()
            }, delay)
        }

        // Type line 1
        for (i in line1.indices) {
            val text = line1.substring(0, i + 1) + "▌"
            post(time) { tv.text = text }
            time += 180
        }
        post(time) { tv.text = line1 }
        time += 800

        // Clear
        post(time) { tv.text = "" }
        time += 400

        // Type line 2
        for (i in line2.indices) {
            val text = line2.substring(0, i + 1) + "▌"
            post(time) { tv.text = text }
            time += 200
        }
        // Final: mark as done, text stays permanently
        post(time) {
            tv.text = line2
            hasPlayedTypewriter = true
            isTypewriterRunning = false
        }
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
        animationToken = Any()
        handler.removeCallbacksAndMessages(null)
        isTypewriterRunning = false
        super.onDestroyView()
        _binding = null
    }
}
