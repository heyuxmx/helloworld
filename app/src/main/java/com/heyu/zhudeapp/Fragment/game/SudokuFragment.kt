package com.heyu.zhudeapp.Fragment.game

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.game.SudokuGenerator
import com.heyu.zhudeapp.util.ThemeManager
import com.heyu.zhudeapp.view.SudokuView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SudokuFragment : Fragment() {

    private lateinit var entryContainer: View
    private lateinit var gameContainer: View
    private lateinit var sudokuView: SudokuView
    private lateinit var tvLives: TextView
    private lateinit var tvDifficultyLabel: TextView
    private lateinit var numberPad: LinearLayout
    private lateinit var btnDraftToggle: MaterialButton

    // Game state
    private var solution = IntArray(81)
    private var userGrid = IntArray(81)
    private var isGiven = BooleanArray(81)
    private var isError = BooleanArray(81)
    private var selectedCell = -1
    private var errorCount = 0
    private var isGameOver = false

    // Draft state
    private var isDraftMode = false
    private var drafts = Array(81) { mutableSetOf<Int>() }

    private val numberButtons = mutableListOf<TextView>()

    // Sound and vibration feedback
    private lateinit var soundPool: SoundPool
    private var soundCorrectId = 0
    private var soundWrongId = 0
    private var soundWinId = 0
    private var soundLoseId = 0
    private lateinit var vibrator: Vibrator

    enum class Difficulty(val clues: Int, val label: String) {
        EASY(46, "简单"), MEDIUM(36, "中等"), HARD(28, "困难")
    }

    private var currentDifficulty = Difficulty.EASY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sudoku, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        entryContainer = view.findViewById(R.id.entry_container)
        gameContainer = view.findViewById(R.id.game_container)
        sudokuView = view.findViewById(R.id.sudoku_view)
        tvLives = view.findViewById(R.id.tv_lives)
        tvDifficultyLabel = view.findViewById(R.id.tv_difficulty_label)
        numberPad = view.findViewById(R.id.number_pad)
        btnDraftToggle = view.findViewById(R.id.btn_draft_toggle)

        val chipGroup = view.findViewById<ChipGroup>(R.id.difficulty_chip_group)

        view.findViewById<View>(R.id.btn_start).setOnClickListener {
            currentDifficulty = when (chipGroup.checkedChipId) {
                R.id.chip_medium -> Difficulty.MEDIUM
                R.id.chip_hard -> Difficulty.HARD
                else -> Difficulty.EASY
            }
            startGame()
        }

        view.findViewById<View>(R.id.btn_new_game).setOnClickListener {
            entryContainer.visibility = View.VISIBLE
            gameContainer.visibility = View.GONE
        }

        view.findViewById<View>(R.id.btn_erase).setOnClickListener { eraseCurrent() }

        btnDraftToggle.setOnClickListener { toggleDraftMode() }

        view.findViewById<View>(R.id.btn_clear_drafts).setOnClickListener { clearAllDrafts() }

        sudokuView.onCellTapped = { index -> selectCell(index) }

        setupNumberPad()

        // Initialize sound and vibration
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
        soundCorrectId = loadSound(R.raw.sudoku_correct)
        soundWrongId = loadSound(R.raw.sudoku_wrong)
        soundWinId = loadSound(R.raw.sudoku_win)
        soundLoseId = loadSound(R.raw.sudoku_lose)
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun loadSound(resId: Int): Int {
        return try {
            soundPool.load(requireContext(), resId, 1)
        } catch (e: Exception) {
            -1
        }
    }

    private fun setupNumberPad() {
        val density = resources.displayMetrics.density
        numberButtons.clear()
        for (i in 1..9) {
            val btn = TextView(requireContext()).apply {
                text = i.toString()
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(Color.parseColor("#E8578A"))
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFF0F5"))
                    cornerRadius = 12 * density
                }
                layoutParams = LinearLayout.LayoutParams(
                    0, (48 * density).toInt(), 1f
                ).apply {
                    marginStart = (3 * density).toInt()
                    marginEnd = (3 * density).toInt()
                }
                setOnClickListener { inputNumber(i) }
            }
            numberPad.addView(btn)
            numberButtons.add(btn)
        }
    }

    private fun startGame() {
        viewLifecycleOwner.lifecycleScope.launch {
            val clues = currentDifficulty.clues
            val (puzzle, sol) = withContext(Dispatchers.Default) {
                SudokuGenerator().generate(clues)
            }
            solution = sol
            puzzle.copyInto(userGrid)
            errorCount = 0
            selectedCell = -1
            isGameOver = false
            isDraftMode = false
            drafts = Array(81) { mutableSetOf() }

            for (i in 0..80) {
                isGiven[i] = puzzle[i] != 0
                isError[i] = false
            }

            tvDifficultyLabel.text = currentDifficulty.label
            updateLivesDisplay()
            updateDraftButton()
            updateBoard()
            updateNumberButtons()

            entryContainer.visibility = View.GONE
            gameContainer.visibility = View.VISIBLE
        }
    }

    private fun selectCell(index: Int) {
        if (isGameOver) return
        selectedCell = index
        updateBoard()
    }

    private fun inputNumber(number: Int) {
        if (selectedCell < 0 || isGiven[selectedCell] || isGameOver) return

        // Draft mode: toggle draft number
        if (isDraftMode) {
            if (userGrid[selectedCell] != 0) return // cell already has a real number
            if (number in drafts[selectedCell]) {
                drafts[selectedCell].remove(number)
            } else {
                drafts[selectedCell].add(number)
            }
            updateBoard()
            return
        }

        // Normal mode
        if (userGrid[selectedCell] == number) return // same number, ignore

        userGrid[selectedCell] = number
        drafts[selectedCell].clear() // clear drafts for this cell

        if (number != solution[selectedCell]) {
            isError[selectedCell] = true
            errorCount++
            updateLivesDisplay()

            playSound(soundWrongId)
            vibrate(50)

            if (errorCount >= 3) {
                isGameOver = true
                updateBoard()
                showResultDialog(false)
                return
            }
        } else {
            isError[selectedCell] = false
            playSound(soundCorrectId)

            // Auto-clear this number from drafts in related cells
            val r = selectedCell / 9
            val c = selectedCell % 9
            for (i in 0..80) {
                val ri = i / 9
                val ci = i % 9
                if (ri == r || ci == c || (ri / 3 == r / 3 && ci / 3 == c / 3)) {
                    drafts[i].remove(number)
                }
            }

            if (userGrid.contentEquals(solution)) {
                isGameOver = true
                updateBoard()
                showResultDialog(true)
                return
            }
        }

        updateBoard()
        updateNumberButtons()
    }

    private fun toggleDraftMode() {
        isDraftMode = !isDraftMode
        updateDraftButton()
    }

    private fun updateDraftButton() {
        val isTech = ThemeManager.isTech(requireContext())
        if (isDraftMode) {
            btnDraftToggle.text = "草稿(开)"
            if (isTech) {
                btnDraftToggle.setTextColor(Color.parseColor("#00D4FF"))
                btnDraftToggle.strokeColor = ColorStateList.valueOf(Color.parseColor("#00D4FF"))
                btnDraftToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A00D4FF"))
            } else {
                btnDraftToggle.setTextColor(Color.parseColor("#E8578A"))
                btnDraftToggle.strokeColor = ColorStateList.valueOf(Color.parseColor("#E8578A"))
                btnDraftToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33E8578A"))
            }
        } else {
            btnDraftToggle.text = "草稿"
            if (isTech) {
                btnDraftToggle.setTextColor(Color.parseColor("#00D4FF"))
                btnDraftToggle.strokeColor = ColorStateList.valueOf(Color.parseColor("#00D4FF"))
            } else {
                btnDraftToggle.setTextColor(Color.parseColor("#E8578A"))
                btnDraftToggle.strokeColor = ColorStateList.valueOf(Color.parseColor("#E8578A"))
            }
            btnDraftToggle.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }
    }

    private fun clearAllDrafts() {
        for (i in 0..80) drafts[i].clear()
        updateBoard()
    }

    private fun eraseCurrent() {
        if (selectedCell < 0 || isGiven[selectedCell] || isGameOver) return
        userGrid[selectedCell] = 0
        isError[selectedCell] = false
        drafts[selectedCell].clear()
        updateBoard()
        updateNumberButtons()
    }

    private fun updateBoard() {
        sudokuView.setBoard(
            userGrid, isGiven, isError, selectedCell,
            drafts.map { it.toSet() }.toTypedArray()
        )
    }

    private fun updateLivesDisplay() {
        val full = (3 - errorCount).coerceAtLeast(0)
        val empty = errorCount.coerceAtMost(3)
        tvLives.text = buildString {
            repeat(full) { append("\u2665 ") }  // filled heart
            repeat(empty) { append("\u2661 ") } // hollow heart
        }.trim()
    }

    private fun updateNumberButtons() {
        for (i in 1..9) {
            val count = userGrid.count { it == i }
            val btn = numberButtons[i - 1]
            val complete = count >= 9
            btn.alpha = if (complete) 0.3f else 1f
            btn.isEnabled = !complete
        }
    }

    // --- Sound & Vibration ---

    private fun playSound(soundId: Int) {
        if (!::soundPool.isInitialized) return
        try {
            if (soundId > 0) {
                soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f)
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

    // --- Result Dialogs (themed) ---

    private fun showResultDialog(isWin: Boolean) {
        if (!isAdded) return
        val isTech = ThemeManager.isTech(requireContext())

        // Play result sound
        playSound(if (isWin) soundWinId else soundLoseId)

        val dialogView = layoutInflater.inflate(R.layout.dialog_sudoku_result, null)
        val iconTv = dialogView.findViewById<TextView>(R.id.tv_result_icon)
        val titleTv = dialogView.findViewById<TextView>(R.id.tv_result_title)
        val messageTv = dialogView.findViewById<TextView>(R.id.tv_result_message)

        if (isWin) {
            iconTv.text = "\u2605" // filled star
            titleTv.text = "恭喜通关！"
            messageTv.text = "太厉害了！完美过关~"
            if (isTech) {
                iconTv.setTextColor(Color.parseColor("#00D4FF"))
                titleTv.setTextColor(Color.WHITE)
                messageTv.setTextColor(Color.parseColor("#AACCDD"))
            } else {
                iconTv.setTextColor(Color.parseColor("#FFD700"))
                titleTv.setTextColor(Color.parseColor("#E8578A"))
                messageTv.setTextColor(Color.parseColor("#666666"))
            }
        } else {
            iconTv.text = "\u2716" // heavy cross
            titleTv.text = "挑战失败"
            messageTv.text = "错误次数已达3次哦~\n再来一局试试？"
            if (isTech) {
                iconTv.setTextColor(Color.parseColor("#FF4444"))
                titleTv.setTextColor(Color.WHITE)
                messageTv.setTextColor(Color.parseColor("#AACCDD"))
            } else {
                iconTv.setTextColor(Color.parseColor("#E03040"))
                titleTv.setTextColor(Color.parseColor("#333333"))
                messageTv.setTextColor(Color.parseColor("#666666"))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("再来一局") { _, _ -> startGame() }
            .setNegativeButton("返回") { _, _ ->
                entryContainer.visibility = View.VISIBLE
                gameContainer.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
