package com.heyu.zhudeapp.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.game.SokobanLevel
import com.heyu.zhudeapp.util.ThemeManager

class SokobanLevelAdapter(
    private val context: Context,
    private val levels: List<SokobanLevel>,
    private val onLevelClick: (SokobanLevel) -> Unit
) : RecyclerView.Adapter<SokobanLevelAdapter.ViewHolder>() {

    private val prefs = context.getSharedPreferences("sokoban_progress", Context.MODE_PRIVATE)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tv_level_number)
        val tvName: TextView = view.findViewById(R.id.tv_level_name)
        val tvDifficulty: TextView = view.findViewById(R.id.tv_level_difficulty)
        val tvBestSteps: TextView = view.findViewById(R.id.tv_best_steps)
        val ivStatus: ImageView = view.findViewById(R.id.iv_status)
        val ivLock: ImageView = view.findViewById(R.id.iv_lock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sokoban_level, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val level = levels[position]
        val isTech = ThemeManager.isTech(context)
        val isCleared = isLevelCleared(level.id)
        val isUnlocked = isLevelUnlocked(level.id)
        val bestSteps = getBestSteps(level.id)

        holder.tvNumber.text = level.id.toString()
        holder.tvName.text = level.name

        // 难度显示
        val stars = buildString { repeat(level.difficulty.stars) { append("\u2605") } }
        holder.tvDifficulty.text = "${level.difficulty.label} $stars"

        // 主题色
        val accentColor = if (isTech) Color.parseColor("#00D4FF") else Color.parseColor("#E8578A")
        holder.tvNumber.background.setTint(
            if (isUnlocked) accentColor else Color.parseColor("#CCCCCC")
        )

        if (isCleared) {
            holder.tvBestSteps.text = "最佳: ${bestSteps}步"
            holder.tvBestSteps.visibility = View.VISIBLE
            holder.ivStatus.visibility = View.GONE
            holder.ivLock.visibility = View.GONE
        } else if (isUnlocked) {
            holder.tvBestSteps.visibility = View.GONE
            holder.ivStatus.visibility = View.GONE
            holder.ivLock.visibility = View.GONE
        } else {
            holder.tvBestSteps.visibility = View.GONE
            holder.ivStatus.visibility = View.GONE
            holder.ivLock.visibility = View.VISIBLE
        }

        holder.itemView.alpha = if (isUnlocked) 1f else 0.5f
        holder.itemView.setOnClickListener {
            if (isUnlocked) onLevelClick(level)
        }
    }

    override fun getItemCount() = levels.size

    fun isLevelCleared(levelId: Int): Boolean =
        prefs.getBoolean("level_${levelId}_cleared", false)

    fun isLevelUnlocked(levelId: Int): Boolean {
        if (levelId == 1) return true
        return prefs.getBoolean("level_${levelId - 1}_cleared", false)
    }

    fun getBestSteps(levelId: Int): Int =
        prefs.getInt("level_${levelId}_best", -1)

    fun refreshProgress() {
        notifyDataSetChanged()
    }
}
