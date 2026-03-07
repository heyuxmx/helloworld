package com.heyu.zhudeapp.Fragment.game

import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.adapter.SokobanLevelAdapter
import com.heyu.zhudeapp.game.SokobanEngine
import com.heyu.zhudeapp.game.SokobanLevel
import com.heyu.zhudeapp.util.ThemeManager
import com.heyu.zhudeapp.view.SokobanView

class SokobanFragment : Fragment() {

    private lateinit var levelSelectContainer: View
    private lateinit var gameContainer: View
    private lateinit var sokobanView: SokobanView
    private lateinit var tvLevelName: TextView
    private lateinit var tvSteps: TextView
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var btnNextLevel: MaterialButton
    private lateinit var btnLevelList: MaterialButton
    private lateinit var rvLevels: RecyclerView

    private var engine: SokobanEngine? = null
    private var currentLevel: SokobanLevel? = null
    private val allLevels = SokobanLevel.getAllLevels()

    private lateinit var adapter: SokobanLevelAdapter

    // Sound
    private lateinit var soundPool: SoundPool
    private var soundPushId = 0
    private var soundCompleteId = 0
    private lateinit var vibrator: Vibrator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sokoban, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        levelSelectContainer = view.findViewById(R.id.level_select_container)
        gameContainer = view.findViewById(R.id.game_container)
        sokobanView = view.findViewById(R.id.sokoban_view)
        tvLevelName = view.findViewById(R.id.tv_level_name)
        tvSteps = view.findViewById(R.id.tv_steps)
        btnUndo = view.findViewById(R.id.btn_undo)
        btnReset = view.findViewById(R.id.btn_reset)
        btnNextLevel = view.findViewById(R.id.btn_next_level)
        btnLevelList = view.findViewById(R.id.btn_level_list)
        rvLevels = view.findViewById(R.id.rv_levels)

        // 关卡列表
        adapter = SokobanLevelAdapter(requireContext(), allLevels) { level ->
            startLevel(level)
        }
        rvLevels.layoutManager = LinearLayoutManager(requireContext())
        rvLevels.adapter = adapter

        // 方向键
        view.findViewById<ImageButton>(R.id.btn_up).setOnClickListener {
            move(SokobanEngine.Direction.UP)
        }
        view.findViewById<ImageButton>(R.id.btn_down).setOnClickListener {
            move(SokobanEngine.Direction.DOWN)
        }
        view.findViewById<ImageButton>(R.id.btn_left).setOnClickListener {
            move(SokobanEngine.Direction.LEFT)
        }
        view.findViewById<ImageButton>(R.id.btn_right).setOnClickListener {
            move(SokobanEngine.Direction.RIGHT)
        }

        btnUndo.setOnClickListener { undo() }
        btnReset.setOnClickListener { resetLevel() }
        btnNextLevel.setOnClickListener { goNextLevel() }
        btnLevelList.setOnClickListener { showLevelSelect() }

        // Sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
        soundPushId = loadSound(R.raw.sokoban_push)
        soundCompleteId = loadSound(R.raw.sokoban_complete)
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        applyTheme()
    }

    private fun loadSound(resId: Int): Int {
        return try {
            soundPool.load(requireContext(), resId, 1)
        } catch (e: Exception) {
            -1
        }
    }

    private fun applyTheme() {
        val isTech = ThemeManager.isTech(requireContext())
        val dpadBtns = listOf(
            view?.findViewById<ImageButton>(R.id.btn_up),
            view?.findViewById<ImageButton>(R.id.btn_down),
            view?.findViewById<ImageButton>(R.id.btn_left),
            view?.findViewById<ImageButton>(R.id.btn_right)
        )
        if (isTech) {
            dpadBtns.forEach { btn ->
                btn?.background?.setTint(Color.parseColor("#1A1A2E"))
                btn?.setColorFilter(Color.parseColor("#00D4FF"))
            }
        }
    }

    private fun startLevel(level: SokobanLevel) {
        currentLevel = level
        engine = SokobanEngine(level)
        tvLevelName.text = "第${level.id}关 ${level.name}"
        btnNextLevel.visibility = View.GONE
        updateBoard()
        updateSteps()

        levelSelectContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
    }

    private fun move(dir: SokobanEngine.Direction) {
        val eng = engine ?: return
        if (eng.isCompleted()) return

        val moved = eng.move(dir)
        if (!moved) return

        if (eng.lastMoveWasPush()) {
            playSound(soundPushId)
            vibrate(30)
        }

        updateBoard()
        updateSteps()

        if (eng.isCompleted()) {
            onLevelComplete()
        }
    }

    private fun undo() {
        val eng = engine ?: return
        if (eng.undo()) {
            updateBoard()
            updateSteps()
        }
    }

    private fun resetLevel() {
        engine?.reset()
        btnNextLevel.visibility = View.GONE
        updateBoard()
        updateSteps()
    }

    private fun goNextLevel() {
        val cur = currentLevel ?: return
        val nextId = cur.id + 1
        val next = allLevels.find { it.id == nextId }
        if (next != null) {
            startLevel(next)
        } else {
            showLevelSelect()
        }
    }

    private fun showLevelSelect() {
        adapter.refreshProgress()
        gameContainer.visibility = View.GONE
        levelSelectContainer.visibility = View.VISIBLE
    }

    private fun updateBoard() {
        val eng = engine ?: return
        sokobanView.setMap(eng.getMap(), eng.rows, eng.cols)
    }

    private fun updateSteps() {
        val eng = engine ?: return
        tvSteps.text = "步数: ${eng.currentSteps}"
    }

    private fun onLevelComplete() {
        val level = currentLevel ?: return
        val eng = engine ?: return
        val steps = eng.currentSteps

        // 保存进度
        val prefs = requireContext().getSharedPreferences("sokoban_progress", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("level_${level.id}_cleared", true)
            val prevBest = prefs.getInt("level_${level.id}_best", Int.MAX_VALUE)
            if (steps < prevBest) {
                putInt("level_${level.id}_best", steps)
            }
            apply()
        }

        playSound(soundCompleteId)
        vibrate(100)

        // 显示下一关按钮
        val hasNext = allLevels.any { it.id == level.id + 1 }
        if (hasNext) {
            btnNextLevel.visibility = View.VISIBLE
        }

        showCompleteDialog(steps)
    }

    private fun showCompleteDialog(steps: Int) {
        if (!isAdded) return
        val level = currentLevel ?: return
        val isTech = ThemeManager.isTech(requireContext())

        val prefs = requireContext().getSharedPreferences("sokoban_progress", Context.MODE_PRIVATE)
        val bestSteps = prefs.getInt("level_${level.id}_best", steps)

        val dialogView = layoutInflater.inflate(R.layout.dialog_sudoku_result, null)
        val iconTv = dialogView.findViewById<TextView>(R.id.tv_result_icon)
        val titleTv = dialogView.findViewById<TextView>(R.id.tv_result_title)
        val messageTv = dialogView.findViewById<TextView>(R.id.tv_result_message)

        iconTv.text = "\u2605"
        titleTv.text = "通关成功！"
        val newRecord = if (steps <= bestSteps) " (新纪录!)" else ""
        messageTv.text = "用了 $steps 步完成$newRecord\n最佳纪录: $bestSteps 步"

        if (isTech) {
            iconTv.setTextColor(Color.parseColor("#00D4FF"))
            titleTv.setTextColor(Color.WHITE)
            messageTv.setTextColor(Color.parseColor("#AACCDD"))
        } else {
            iconTv.setTextColor(Color.parseColor("#FFD700"))
            titleTv.setTextColor(Color.parseColor("#E8578A"))
            messageTv.setTextColor(Color.parseColor("#666666"))
        }

        val hasNext = allLevels.any { it.id == level.id + 1 }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)

        if (hasNext) {
            builder.setPositiveButton("下一关") { _, _ -> goNextLevel() }
            builder.setNegativeButton("关卡列表") { _, _ -> showLevelSelect() }
        } else {
            builder.setPositiveButton("全部通关！") { _, _ -> showLevelSelect() }
        }

        builder.show()
    }

    // --- Sound & Vibration ---

    private fun playSound(soundId: Int) {
        if (!::soundPool.isInitialized) return
        try {
            if (soundId > 0) {
                soundPool.play(soundId, 0.6f, 0.6f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(milliseconds: Long) {
        if (!::vibrator.isInitialized) return
        try {
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
