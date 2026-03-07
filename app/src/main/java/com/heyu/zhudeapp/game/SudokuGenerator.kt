package com.heyu.zhudeapp.game

class SudokuGenerator {

    fun generate(cluesCount: Int): Pair<IntArray, IntArray> {
        val board = Array(9) { IntArray(9) }
        fillBoard(board)

        val solution = IntArray(81) { board[it / 9][it % 9] }

        // Remove cells to create puzzle
        val indices = (0 until 81).toMutableList().apply { shuffle() }
        val toRemove = 81 - cluesCount
        var removed = 0
        for (idx in indices) {
            if (removed >= toRemove) break
            board[idx / 9][idx % 9] = 0
            removed++
        }

        val puzzle = IntArray(81) { board[it / 9][it % 9] }
        return puzzle to solution
    }

    private fun fillBoard(board: Array<IntArray>): Boolean {
        for (i in 0 until 81) {
            val r = i / 9
            val c = i % 9
            if (board[r][c] == 0) {
                for (n in (1..9).shuffled()) {
                    if (isValid(board, r, c, n)) {
                        board[r][c] = n
                        if (fillBoard(board)) return true
                        board[r][c] = 0
                    }
                }
                return false
            }
        }
        return true
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (c in 0..8) if (board[row][c] == num) return false
        for (r in 0..8) if (board[r][col] == num) return false
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br..br + 2) for (c in bc..bc + 2) {
            if (board[r][c] == num) return false
        }
        return true
    }
}
