package com.heyu.zhudeapp.Fragment.game

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.heyu.zhudeapp.R

class GameHubFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "game_hub"
        private const val KEY_LAST_GAME = "last_game"
        private const val GAME_SUDOKU = "sudoku"
        private const val GAME_SOKOBAN = "sokoban"
    }

    private lateinit var btnSwitchGame: MaterialButton
    private var currentGame = GAME_SUDOKU

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_game_hub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSwitchGame = view.findViewById(R.id.btn_switch_game)

        // 恢复上次选择的游戏
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentGame = prefs.getString(KEY_LAST_GAME, GAME_SUDOKU) ?: GAME_SUDOKU

        showGame(currentGame, false)

        btnSwitchGame.setOnClickListener { showGameMenu(it) }
    }

    private fun showGameMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "数独挑战")
        popup.menu.add(0, 2, 1, "推箱子")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showGame(GAME_SUDOKU, true)
                2 -> showGame(GAME_SOKOBAN, true)
            }
            true
        }
        popup.show()
    }

    private fun showGame(game: String, save: Boolean) {
        currentGame = game

        val fragment: Fragment = when (game) {
            GAME_SOKOBAN -> SokobanFragment()
            else -> SudokuFragment()
        }

        btnSwitchGame.text = when (game) {
            GAME_SOKOBAN -> "推箱子 \u25BE"
            else -> "数独 \u25BE"
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.game_fragment_container, fragment)
            .commit()

        if (save) {
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_GAME, game).apply()
        }
    }
}
