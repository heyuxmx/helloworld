package com.heyu.zhudeapp.game

/**
 * 推箱子关卡数据
 *
 * 地图元素:
 * 0 = 空(不可达)
 * 1 = 地板
 * 2 = 墙壁
 * 3 = 目标点
 * 4 = 箱子(在地板上)
 * 5 = 箱子在目标点上
 * 6 = 玩家(在地板上)
 * 7 = 玩家在目标点上
 */
data class SokobanLevel(
    val id: Int,
    val name: String,
    val difficulty: Difficulty,
    val rows: Int,
    val cols: Int,
    val map: IntArray
) {
    enum class Difficulty(val label: String, val stars: Int) {
        EASY("入门", 1),
        MEDIUM("进阶", 2),
        HARD("挑战", 3)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SokobanLevel) return false
        return id == other.id
    }

    override fun hashCode() = id

    companion object {
        fun getAllLevels(): List<SokobanLevel> = listOf(
            // ===== 入门 (5关, 1-2个箱子) =====

            // 第1关: 最简单，1个箱子直推
            level(1, "初次尝试", Difficulty.EASY, 5, 5, """
                0 2 2 2 0
                2 1 1 1 2
                2 6 4 3 2
                2 1 1 1 2
                0 2 2 2 0
            """),

            // 第2关: 1个箱子，需要拐弯推
            level(2, "小试身手", Difficulty.EASY, 6, 6, """
                2 2 2 2 2 2
                2 6 1 1 1 2
                2 1 2 1 1 2
                2 1 1 4 1 2
                2 1 1 1 3 2
                2 2 2 2 2 2
            """),

            // 第3关: 2个箱子，纵向推
            level(3, "双箱出击", Difficulty.EASY, 6, 5, """
                2 2 2 2 2
                2 1 1 6 2
                2 1 4 1 2
                2 1 4 1 2
                2 1 3 3 2
                2 2 2 2 2
            """),

            // 第4关: 2个箱子，开放空间
            level(4, "空间思考", Difficulty.EASY, 6, 6, """
                2 2 2 2 2 2
                2 6 1 1 1 2
                2 1 4 1 1 2
                2 1 1 4 1 2
                2 1 3 3 1 2
                2 2 2 2 2 2
            """),

            // 第5关: 2个箱子，需要分别推到不同方向
            level(5, "左右逢源", Difficulty.EASY, 7, 6, """
                2 2 2 2 2 2
                2 6 1 1 1 2
                2 1 4 1 1 2
                2 1 1 1 1 2
                2 1 1 4 1 2
                2 3 1 1 3 2
                2 2 2 2 2 2
            """),

            // ===== 进阶 (5关, 3个箱子) =====

            // 第6关: 3个箱子，简单推下
            level(6, "三足鼎立", Difficulty.MEDIUM, 6, 6, """
                0 2 2 2 2 0
                2 2 1 1 2 0
                2 1 4 4 2 2
                2 1 3 3 1 2
                2 1 6 4 3 2
                2 2 2 2 2 2
            """),

            // 第7关: 3个箱子推到顶部
            level(7, "仰望星空", Difficulty.MEDIUM, 7, 7, """
                2 2 2 2 2 2 2
                2 3 1 3 1 3 2
                2 1 1 1 1 1 2
                2 1 1 4 1 1 2
                2 1 4 1 4 1 2
                2 6 1 1 1 1 2
                2 2 2 2 2 2 2
            """),

            // 第8关: 3个箱子，有一面墙
            level(8, "绕墙而行", Difficulty.MEDIUM, 7, 7, """
                2 2 2 2 2 2 2
                2 1 1 1 1 1 2
                2 1 4 1 4 1 2
                2 1 1 1 1 1 2
                2 1 4 1 6 1 2
                2 1 3 3 3 1 2
                2 2 2 2 2 2 2
            """),

            // 第9关: 3个箱子，中间有柱子
            level(9, "柱间迂回", Difficulty.MEDIUM, 8, 7, """
                2 2 2 2 2 2 2
                2 1 1 1 1 1 2
                2 1 4 1 1 1 2
                2 1 1 2 1 1 2
                2 1 1 1 1 1 2
                2 1 1 4 4 1 2
                2 1 3 3 3 6 2
                2 2 2 2 2 2 2
            """),

            // 第10关: 3个箱子，双柱迷宫
            level(10, "双柱迷宫", Difficulty.MEDIUM, 8, 7, """
                2 2 2 2 2 2 2
                2 6 1 1 1 1 2
                2 1 4 1 1 1 2
                2 1 1 2 1 1 2
                2 1 1 1 1 1 2
                2 1 1 4 1 4 2
                2 1 3 3 3 1 2
                2 2 2 2 2 2 2
            """),

            // ===== 挑战 (5关, 4-5个箱子) =====

            // 第11关: 4个箱子，对称布局
            level(11, "四方汇聚", Difficulty.HARD, 8, 7, """
                2 2 2 2 2 2 2
                2 1 1 1 1 1 2
                2 1 4 1 4 1 2
                2 1 1 1 1 1 2
                2 1 4 1 4 1 2
                2 1 1 1 1 1 2
                2 3 3 6 3 3 2
                2 2 2 2 2 2 2
            """),

            // 第12关: 4个箱子，单柱障碍
            level(12, "暗礁潜行", Difficulty.HARD, 8, 8, """
                2 2 2 2 2 2 2 2
                2 1 1 1 1 1 1 2
                2 1 4 1 4 1 1 2
                2 1 1 1 1 1 1 2
                2 1 1 2 1 1 1 2
                2 1 4 1 1 4 1 2
                2 3 3 1 3 3 6 2
                2 2 2 2 2 2 2 2
            """),

            // 第13关: 4个箱子，双柱挑战
            level(13, "双壁夹击", Difficulty.HARD, 8, 8, """
                2 2 2 2 2 2 2 2
                2 6 1 1 1 1 1 2
                2 1 4 1 1 4 1 2
                2 1 1 2 2 1 1 2
                2 1 1 1 1 1 1 2
                2 1 4 1 1 4 1 2
                2 3 3 1 1 3 3 2
                2 2 2 2 2 2 2 2
            """),

            // 第14关: 4个箱子，菱形空间
            level(14, "菱形空间", Difficulty.HARD, 9, 9, """
                0 0 2 2 2 2 2 0 0
                0 2 2 3 1 3 2 2 0
                2 2 1 1 1 1 1 2 2
                2 1 1 4 1 4 1 1 2
                2 1 1 1 6 1 1 1 2
                2 1 1 4 1 4 1 1 2
                2 2 1 1 1 1 1 2 2
                0 2 2 3 1 3 2 2 0
                0 0 2 2 2 2 2 0 0
            """),

            // 第15关: 5个箱子，终极对称
            level(15, "终极挑战", Difficulty.HARD, 9, 9, """
                2 2 2 2 2 2 2 2 2
                2 3 1 1 3 1 1 3 2
                2 1 1 1 1 1 1 1 2
                2 1 1 4 1 4 1 1 2
                2 1 1 1 4 1 1 1 2
                2 1 1 4 1 4 1 1 2
                2 1 1 1 1 1 1 1 2
                2 3 1 1 6 1 1 3 2
                2 2 2 2 2 2 2 2 2
            """)
        )

        /**
         * 从 level0.txt 格式解析关卡列表
         *
         * level0.txt 编码（JavaScript 数组格式）:
         * - 0 = 地板（墙内）或空白（墙外）
         * - 1 = 围墙
         * - 2 = 目标点
         * - 3 = 箱子（在地板上）
         * - 4 = 人物（玩家）
         * - 5 = 箱子在目标点上
         *
         * 解析后映射到 SokobanLevel 编码:
         * 0(墙外空白)→0, 0(墙内地板)→1, 1→2, 2→3, 3→4, 4→6, 5→5
         * 墙内/墙外的区分使用从边界泛洪填充算法完成。
         */
        fun parseLevel0Array(content: String): List<SokobanLevel> {
            val levels = mutableListOf<SokobanLevel>()
            var id = 1

            // 匹配每个 levels[n]=[...]; 块
            val levelPattern = Regex("""levels\[\d+]=\[([\s\S]*?)];\s*""")
            for (match in levelPattern.findAll(content)) {
                val body = match.groupValues[1]

                // 解析每行 [a,b,c,...] 数字数组
                val rowPattern = Regex("""\[([0-9,\s]+)]""")
                val grid = mutableListOf<IntArray>()
                for (rowMatch in rowPattern.findAll(body)) {
                    val nums = rowMatch.groupValues[1]
                        .split(",")
                        .map { it.trim().toIntOrNull() ?: 0 }
                        .toIntArray()
                    grid.add(nums)
                }
                if (grid.isEmpty()) continue

                val rawRows = grid.size
                val rawCols = grid.maxOf { it.size }

                // 第一步：初步映射，0 先标记为 -1（待定：墙外空白 or 墙内地板）
                val rawMap = IntArray(rawRows * rawCols)
                for (r in 0 until rawRows) {
                    val row = grid[r]
                    for (c in 0 until rawCols) {
                        val v = if (c < row.size) row[c] else 0
                        rawMap[r * rawCols + c] = when (v) {
                            0 -> -1  // 待定
                            1 -> 2   // 围墙 → 墙壁
                            2 -> 3   // 目标点
                            3 -> 4   // 箱子在地板上
                            4 -> 6   // 玩家在地板上
                            5 -> 5   // 箱子在目标点上
                            else -> 0
                        }
                    }
                }

                // 第二步：从边界泛洪填充，找出所有墙外的 -1（标记为已访问）
                val visited = BooleanArray(rawRows * rawCols)
                val queue = ArrayDeque<Int>()
                for (r in 0 until rawRows) {
                    for (c in 0 until rawCols) {
                        val idx = r * rawCols + c
                        if ((r == 0 || r == rawRows - 1 || c == 0 || c == rawCols - 1) &&
                            rawMap[idx] == -1 && !visited[idx]) {
                            visited[idx] = true
                            queue.add(idx)
                        }
                    }
                }
                while (queue.isNotEmpty()) {
                    val pos = queue.removeFirst()
                    val pr = pos / rawCols
                    val pc = pos % rawCols
                    for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                        val nr = pr + dr
                        val nc = pc + dc
                        if (nr < 0 || nr >= rawRows || nc < 0 || nc >= rawCols) continue
                        val ni = nr * rawCols + nc
                        if (!visited[ni] && rawMap[ni] == -1) {
                            visited[ni] = true
                            queue.add(ni)
                        }
                    }
                }

                // 第三步：墙外 -1 → 0（空白），墙内 -1 → 1（地板）
                for (i in rawMap.indices) {
                    if (rawMap[i] == -1) {
                        rawMap[i] = if (visited[i]) 0 else 1
                    }
                }

                // 第四步：裁剪到非零区域的最小包围盒
                var minR = rawRows; var maxR = -1; var minC = rawCols; var maxC = -1
                for (r in 0 until rawRows) {
                    for (c in 0 until rawCols) {
                        if (rawMap[r * rawCols + c] != 0) {
                            if (r < minR) minR = r
                            if (r > maxR) maxR = r
                            if (c < minC) minC = c
                            if (c > maxC) maxC = c
                        }
                    }
                }
                if (maxR < 0) continue

                val rows = maxR - minR + 1
                val cols = maxC - minC + 1
                val map = IntArray(rows * cols)
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        map[r * cols + c] = rawMap[(r + minR) * rawCols + (c + minC)]
                    }
                }

                // 验证：必须有玩家和箱子
                val playerCount = map.count { it == 6 || it == 7 }
                val boxCount = map.count { it == 4 || it == 5 }
                if (playerCount == 0 || boxCount == 0) continue

                val difficulty = when {
                    boxCount <= 2 -> Difficulty.EASY
                    boxCount <= 4 -> Difficulty.MEDIUM
                    else -> Difficulty.HARD
                }

                levels.add(SokobanLevel(id, "第${id}关", difficulty, rows, cols, map))
                id++
            }

            return levels
        }

        private fun level(
            id: Int, name: String, diff: Difficulty,
            rows: Int, cols: Int, mapStr: String
        ): SokobanLevel {
            val values = mapStr.trim().split("\\s+".toRegex()).map { it.toInt() }
            require(values.size == rows * cols) {
                "Level $id: expected ${rows * cols} cells but got ${values.size}"
            }
            return SokobanLevel(id, name, diff, rows, cols, values.toIntArray())
        }
    }
}
