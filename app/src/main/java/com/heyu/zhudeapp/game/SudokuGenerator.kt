package com.heyu.zhudeapp.game

class SudokuGenerator {

    fun generate(cluesCount: Int): Pair<IntArray, IntArray> {
        val board = Array(9) { IntArray(9) }
        fillBoard(board)

        val solution = IntArray(81) { board[it / 9][it % 9] }

        val toRemove = 81 - cluesCount

        // Try up to 3 times with different random orders to reach target removal count
        var bestPuzzle: Array<IntArray>? = null
        var bestRemoved = 0

        for (attempt in 0 until 3) {
            val puzzle = Array(9) { r -> IntArray(9) { c -> board[r][c] } }
            val indices = (0 until 81).toMutableList().apply { shuffle() }
            var removed = 0

            for (idx in indices) {
                if (removed >= toRemove) break

                val r = idx / 9
                val c = idx % 9
                val savedValue = puzzle[r][c]
                if (savedValue == 0) continue // Already empty

                // Try removing this cell
                puzzle[r][c] = 0

                // Check if puzzle still has unique solution
                if (hasUniqueSolution(puzzle)) {
                    removed++
                } else {
                    // Restore the cell if removal creates multiple solutions
                    puzzle[r][c] = savedValue
                }
            }

            // Keep track of the best attempt (most cells removed)
            if (removed > bestRemoved) {
                bestRemoved = removed
                bestPuzzle = puzzle

                // If we reached target, we can stop early
                if (removed >= toRemove) {
                    break
                }
            }
        }

        // Use the best puzzle we found (may have more clues than target)
        val finalPuzzle = if (bestPuzzle != null) {
            IntArray(81) { bestPuzzle[it / 9][it % 9] }
        } else {
            // Fallback: use original board (full solution)
            IntArray(81) { board[it / 9][it % 9] }
        }

        return finalPuzzle to solution
    }

    /**
     * Check if a sudoku puzzle has exactly one solution.
     * Returns true if there is exactly one solution, false if multiple solutions or no solution.
     */
    private fun hasUniqueSolution(board: Array<IntArray>): Boolean {
        val copy = Array(9) { r -> IntArray(9) { c -> board[r][c] } }
        var solutionCount = 0

        // Helper to get candidate numbers for a cell
        fun getCandidates(board: Array<IntArray>, row: Int, col: Int): List<Int> {
            val candidates = mutableListOf<Int>()
            for (n in 1..9) {
                if (isValid(board, row, col, n)) {
                    candidates.add(n)
                }
            }
            return candidates
        }

        // Find the cell with fewest candidates (most constrained)
        fun findBestCell(board: Array<IntArray>): Pair<Int, Int>? {
            var bestRow = -1
            var bestCol = -1
            var bestCandidates = 10 // More than max possible candidates (9)

            for (r in 0..8) {
                for (c in 0..8) {
                    if (board[r][c] == 0) {
                        val candidates = getCandidates(board, r, c)
                        if (candidates.size < bestCandidates) {
                            bestCandidates = candidates.size
                            bestRow = r
                            bestCol = c
                            // If we find a cell with only 1 candidate, we can stop early
                            if (bestCandidates == 1) {
                                return bestRow to bestCol
                            }
                        }
                    }
                }
            }

            return if (bestRow != -1) bestRow to bestCol else null
        }

        fun countSolutions(board: Array<IntArray>) {
            val cell = findBestCell(board)
            if (cell == null) {
                // No empty cells - found a solution
                solutionCount++
                return
            }

            val (r, c) = cell
            val candidates = getCandidates(board, r, c)

            for (n in candidates) {
                board[r][c] = n
                countSolutions(board)
                board[r][c] = 0

                // Early termination if we already found 2 solutions
                if (solutionCount >= 2) {
                    return
                }
            }
        }

        countSolutions(copy)
        return solutionCount == 1
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
