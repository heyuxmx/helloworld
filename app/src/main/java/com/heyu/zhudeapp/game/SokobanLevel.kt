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
