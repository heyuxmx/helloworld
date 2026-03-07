package com.heyu.zhudeapp.Fragment.game

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.game.SudokuGenerator
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

    // Game state
    private var solution = IntArray(81)
    private var userGrid = IntArray(81)
    private var isGiven = BooleanArray(81)
    private var isError = BooleanArray(81)
    private var selectedCell = -1
    private var errorCount = 0
    private var isGameOver = false

    private val numberButtons = mutableListOf<TextView>()

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

        sudokuView.onCellTapped = { index -> selectCell(index) }

        setupNumberPad()
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
            for (i in 0..80) {
                isGiven[i] = puzzle[i] != 0
                isError[i] = false
            }

            tvDifficultyLabel.text = currentDifficulty.label
            updateLivesDisplay()
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
        if (userGrid[selectedCell] == number) return // same number, ignore

        userGrid[selectedCell] = number

        if (number != solution[selectedCell]) {
            isError[selectedCell] = true
            errorCount++
            updateLivesDisplay()
            if (errorCount >= 3) {
                isGameOver = true
                updateBoard()
                showGameOverDialog()
                return
            }
        } else {
            isError[selectedCell] = false
            if (userGrid.contentEquals(solution)) {
                isGameOver = true
                updateBoard()
                showWinDialog()
                return
            }
        }

        updateBoard()
        updateNumberButtons()
    }

    private fun eraseCurrent() {
        if (selectedCell < 0 || isGiven[selectedCell] || isGameOver) return
        userGrid[selectedCell] = 0
        isError[selectedCell] = false
        updateBoard()
        updateNumberButtons()
    }

    private fun updateBoard() {
        sudokuView.setBoard(userGrid, isGiven, isError, selectedCell)
    }

    private fun updateLivesDisplay() {
        val full = (3 - errorCount).coerceAtLeast(0)
        val empty = errorCount.coerceAtMost(3)
        tvLives.text = buildString {
            repeat(full) { append("\u2665 ") }  // ♥
            repeat(empty) { append("\u2661 ") } // ♡
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

    private fun showGameOverDialog() {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("挑战失败")
            .setMessage("错误次数已达3次哦~\n再来一局试试？")
            .setPositiveButton("再来一局") { _, _ -> startGame() }
            .setNegativeButton("返回") { _, _ ->
                entryContainer.visibility = View.VISIBLE
                gameContainer.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }

    private fun showWinDialog() {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("恭喜通关！")
            .setMessage("太厉害了！完美过关~")
            .setPositiveButton("再来一局") { _, _ -> startGame() }
            .setNegativeButton("返回") { _, _ ->
                entryContainer.visibility = View.VISIBLE
                gameContainer.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }
}
