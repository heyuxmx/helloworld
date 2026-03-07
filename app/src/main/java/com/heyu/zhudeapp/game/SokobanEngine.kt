package com.heyu.zhudeapp.game

/**
 * 推箱子游戏引擎
 * 处理移动逻辑、碰撞检测、胜利判定、撤销功能
 */
class SokobanEngine(private val level: SokobanLevel) {

    // 当前地图状态 (可变)
    private var map: IntArray = level.map.copyOf()
    private var playerPos: Int = -1
    private var steps: Int = 0
    private val history = mutableListOf<MoveRecord>()

    // 移动记录，用于撤销
    private data class MoveRecord(
        val playerFrom: Int,
        val playerTo: Int,
        val boxFrom: Int,      // -1 表示没推箱子
        val boxTo: Int,
        val prevPlayerCell: Int,
        val prevDestCell: Int,
        val prevBoxDestCell: Int
    )

    enum class Direction(val dr: Int, val dc: Int) {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1)
    }

    val rows get() = level.rows
    val cols get() = level.cols
    val currentSteps get() = steps
    val canUndo get() = history.isNotEmpty()

    init {
        findPlayer()
    }

    private fun findPlayer() {
        for (i in map.indices) {
            if (map[i] == 6 || map[i] == 7) {
                playerPos = i
                return
            }
        }
    }

    fun getCell(index: Int): Int = map[index]
    fun getCell(row: Int, col: Int): Int = map[row * cols + col]

    fun getMap(): IntArray = map.copyOf()

    /**
     * 尝试向指定方向移动
     * @return true 如果移动成功
     */
    fun move(dir: Direction): Boolean {
        val pr = playerPos / cols
        val pc = playerPos % cols
        val nr = pr + dir.dr
        val nc = pc + dir.dc

        // 边界检查
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return false

        val nextPos = nr * cols + nc
        val nextCell = map[nextPos]

        // 目标是墙壁或空地，不能移动
        if (nextCell == 2 || nextCell == 0) return false

        // 目标是箱子(4)或箱子在目标点上(5)
        if (nextCell == 4 || nextCell == 5) {
            val boxR = nr + dir.dr
            val boxC = nc + dir.dc
            if (boxR < 0 || boxR >= rows || boxC < 0 || boxC >= cols) return false

            val boxNextPos = boxR * cols + boxC
            val boxNextCell = map[boxNextPos]

            // 箱子后面必须是地板(1)或目标点(3)
            if (boxNextCell != 1 && boxNextCell != 3) return false

            // 记录移动前状态
            val record = MoveRecord(
                playerFrom = playerPos,
                playerTo = nextPos,
                boxFrom = nextPos,
                boxTo = boxNextPos,
                prevPlayerCell = map[playerPos],
                prevDestCell = map[nextPos],
                prevBoxDestCell = map[boxNextPos]
            )
            history.add(record)

            // 移动箱子
            map[boxNextPos] = if (boxNextCell == 3) 5 else 4  // 箱子到目标点=5, 否则=4

            // 移动玩家到箱子原位
            map[nextPos] = if (nextCell == 5) 7 else 6  // 箱子在目标点上→玩家在目标点, 否则玩家在地板

            // 玩家原位恢复
            map[playerPos] = if (map[playerPos] == 7) 3 else 1  // 玩家在目标点→目标点, 否则地板
            // 修正：用记录的原始值判断
            map[playerPos] = if (record.prevPlayerCell == 7) 3 else 1

            playerPos = nextPos
            steps++
            return true
        }

        // 目标是地板(1)或目标点(3)
        if (nextCell == 1 || nextCell == 3) {
            val record = MoveRecord(
                playerFrom = playerPos,
                playerTo = nextPos,
                boxFrom = -1,
                boxTo = -1,
                prevPlayerCell = map[playerPos],
                prevDestCell = map[nextPos],
                prevBoxDestCell = 0
            )
            history.add(record)

            map[nextPos] = if (nextCell == 3) 7 else 6
            map[playerPos] = if (record.prevPlayerCell == 7) 3 else 1

            playerPos = nextPos
            steps++
            return true
        }

        return false
    }

    /**
     * 是否推了箱子（用于播放不同音效）
     */
    fun lastMoveWasPush(): Boolean {
        return history.lastOrNull()?.boxFrom != -1
    }

    /**
     * 撤销上一步
     */
    fun undo(): Boolean {
        if (history.isEmpty()) return false
        val record = history.removeAt(history.lastIndex)

        // 恢复箱子
        if (record.boxFrom != -1) {
            map[record.boxTo] = record.prevBoxDestCell
            map[record.boxFrom] = record.prevDestCell
        } else {
            map[record.playerTo] = record.prevDestCell
        }

        // 恢复玩家
        map[record.playerFrom] = record.prevPlayerCell
        playerPos = record.playerFrom
        steps--

        return true
    }

    /**
     * 重置当前关卡
     */
    fun reset() {
        level.map.copyInto(map)
        playerPos = -1
        steps = 0
        history.clear()
        findPlayer()
    }

    /**
     * 检查是否通关（所有目标点都被箱子覆盖）
     */
    fun isCompleted(): Boolean {
        // 没有剩余的空目标点(3)和玩家在目标点上(7)
        return map.none { it == 3 || it == 7 }
    }

    /**
     * 获取玩家位置
     */
    fun getPlayerPosition(): Int = playerPos
}
