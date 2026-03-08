package com.heyu.zhudeapp.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.util.ThemeManager

class SokobanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var map: IntArray = IntArray(0)
    private var rows = 0
    private var cols = 0
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private val density = context.resources.displayMetrics.density

    // 缓存 drawable
    private var playerDrawable: Drawable? = null
    private var boxDrawable: Drawable? = null
    private var wallDrawable: Drawable? = null
    private var targetDrawable: Drawable? = null
    private var floorDrawable: Drawable? = null

    // 画笔
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxOnTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 动画相关
    private var animPlayerPos = -1
    private var animProgress = 1f

    init {
        loadDrawables()
    }

    private fun loadDrawables() {
        playerDrawable = safeLoadDrawable(R.drawable.ic_sokoban_player)
        boxDrawable = safeLoadDrawable(R.drawable.ic_box)
        wallDrawable = safeLoadDrawable(R.drawable.ic_sokoban_wall)
        targetDrawable = safeLoadDrawable(R.drawable.ic_sokoban_target)
        floorDrawable = safeLoadDrawable(R.drawable.ic_sokoban_floor)
    }

    private fun safeLoadDrawable(resId: Int): Drawable? {
        return try {
            ContextCompat.getDrawable(context, resId)
        } catch (e: Exception) {
            null
        }
    }

    fun setMap(map: IntArray, rows: Int, cols: Int) {
        this.map = map.copyOf()
        this.rows = rows
        this.cols = cols
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (rows == 0 || cols == 0) {
            setMeasuredDimension(availableWidth, availableHeight)
            return
        }

        // 计算按宽度和高度分别适配时的单元格大小
        val cellSizeByWidth = availableWidth.toFloat() / cols
        val cellSizeByHeight = availableHeight.toFloat() / rows

        // 选择较小的单元格大小，确保地图完整显示
        cellSize = minOf(cellSizeByWidth, cellSizeByHeight)

        // 计算实际地图尺寸
        val actualWidth = (cellSize * cols).toInt()
        val actualHeight = (cellSize * rows).toInt()

        // 居中显示
        offsetX = (availableWidth - actualWidth) / 2f
        offsetY = (availableHeight - actualHeight) / 2f

        setMeasuredDimension(availableWidth, availableHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (map.isEmpty() || rows == 0) return

        val isTech = ThemeManager.isTech(context)
        setupPaints(isTech)

        canvas.save()
        canvas.translate(offsetX, offsetY)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val cell = map[idx]
                val x = c * cellSize
                val y = r * cellSize
                drawCell(canvas, cell, x, y, isTech)
            }
        }

        canvas.restore()
    }

    private fun setupPaints(isTech: Boolean) {
        if (isTech) {
            floorPaint.color = Color.parseColor("#1A1A2E")
            wallPaint.color = Color.parseColor("#2D2D44")
            targetPaint.color = Color.parseColor("#1A1A2E")
            boxPaint.color = Color.parseColor("#FF8C42")
            boxOnTargetPaint.color = Color.parseColor("#00E676")
            playerPaint.color = Color.parseColor("#00D4FF")
            gridPaint.color = Color.parseColor("#252540")
            gridPaint.style = Paint.Style.STROKE
            gridPaint.strokeWidth = 1f
            shadowPaint.color = Color.parseColor("#40000000")
            targetMarkerPaint.color = Color.parseColor("#00D4FF")
            targetMarkerPaint.style = Paint.Style.STROKE
            targetMarkerPaint.strokeWidth = 2.5f * density
        } else {
            floorPaint.color = Color.parseColor("#FFF5E6")
            wallPaint.color = Color.parseColor("#8B7355")
            targetPaint.color = Color.parseColor("#FFF5E6")
            boxPaint.color = Color.parseColor("#D4894A")
            boxOnTargetPaint.color = Color.parseColor("#66BB6A")
            playerPaint.color = Color.parseColor("#E8578A")
            gridPaint.color = Color.parseColor("#F0E0D0")
            gridPaint.style = Paint.Style.STROKE
            gridPaint.strokeWidth = 1f
            shadowPaint.color = Color.parseColor("#20000000")
            targetMarkerPaint.color = Color.parseColor("#E8578A")
            targetMarkerPaint.style = Paint.Style.STROKE
            targetMarkerPaint.strokeWidth = 2.5f * density
        }
    }

    private fun drawCell(canvas: Canvas, cell: Int, x: Float, y: Float, isTech: Boolean) {
        val fullRect = RectF(x, y, x + cellSize, y + cellSize)
        val padding = cellSize * 0.06f
        val paddedRect = RectF(x + padding, y + padding, x + cellSize - padding, y + cellSize - padding)
        val cornerRadius = cellSize * 0.15f

        when (cell) {
            0 -> { /* 空地，不绘制 */ }
            1 -> drawFloor(canvas, fullRect, cornerRadius)
            2 -> drawWall(canvas, fullRect, cornerRadius, isTech)
            3 -> drawTarget(canvas, fullRect, cornerRadius)
            4 -> {
                drawFloor(canvas, fullRect, cornerRadius)
                drawBox(canvas, fullRect, cornerRadius, false, isTech)
            }
            5 -> {
                drawTarget(canvas, fullRect, cornerRadius)
                drawBox(canvas, fullRect, cornerRadius, true, isTech)
            }
            6 -> {
                drawFloor(canvas, fullRect, cornerRadius)
                drawPlayer(canvas, fullRect)
            }
            7 -> {
                drawTarget(canvas, fullRect, cornerRadius)
                drawPlayer(canvas, fullRect)
            }
        }
    }

    private fun drawFloor(canvas: Canvas, rect: RectF, radius: Float) {
        if (floorDrawable != null) {
            floorDrawable!!.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
            floorDrawable!!.draw(canvas)
        } else {
            canvas.drawRoundRect(rect, radius, radius, floorPaint)
            canvas.drawRoundRect(rect, radius, radius, gridPaint)
        }
    }

    private fun drawWall(canvas: Canvas, rect: RectF, radius: Float, isTech: Boolean) {
        if (wallDrawable != null) {
            wallDrawable!!.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
            wallDrawable!!.draw(canvas)
        } else {
            // 绘制3D效果墙壁
            val depth = cellSize * 0.08f
            val shadowRect = RectF(rect.left + depth, rect.top + depth, rect.right + depth, rect.bottom + depth)
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)
            canvas.drawRoundRect(rect, radius, radius, wallPaint)

            // 墙壁纹理线
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isTech) Color.parseColor("#3D3D5C") else Color.parseColor("#9E8B72")
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
            }
            val midY = (rect.top + rect.bottom) / 2
            canvas.drawLine(rect.left + radius, midY, rect.right - radius, midY, linePaint)
            val q1 = rect.top + (rect.bottom - rect.top) * 0.33f
            val q3 = rect.top + (rect.bottom - rect.top) * 0.66f
            val midX = (rect.left + rect.right) / 2
            canvas.drawLine(midX, rect.top + radius * 0.5f, midX, q1, linePaint)
            canvas.drawLine(midX, q3, midX, rect.bottom - radius * 0.5f, linePaint)
        }
    }

    private fun drawTarget(canvas: Canvas, rect: RectF, radius: Float) {
        // 先画地板
        if (floorDrawable != null) {
            floorDrawable!!.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
            floorDrawable!!.draw(canvas)
        } else {
            canvas.drawRoundRect(rect, radius, radius, floorPaint)
        }

        // 画目标标记
        if (targetDrawable != null) {
            val inset = cellSize * 0.2f
            val tRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
            targetDrawable!!.setBounds(tRect.left.toInt(), tRect.top.toInt(), tRect.right.toInt(), tRect.bottom.toInt())
            targetDrawable!!.draw(canvas)
        } else {
            val cx = (rect.left + rect.right) / 2
            val cy = (rect.top + rect.bottom) / 2
            val r = cellSize * 0.18f
            canvas.drawCircle(cx, cy, r, targetMarkerPaint)
            // 十字
            val cr = cellSize * 0.1f
            canvas.drawLine(cx - cr, cy, cx + cr, cy, targetMarkerPaint)
            canvas.drawLine(cx, cy - cr, cx, cy + cr, targetMarkerPaint)
        }
    }

    private fun drawBox(canvas: Canvas, rect: RectF, radius: Float, onTarget: Boolean, isTech: Boolean) {
        val boxRect = RectF(rect)

        if (boxDrawable != null && !onTarget) {
            // 阴影
            val depth = cellSize * 0.05f
            val shadowRect = RectF(boxRect.left + depth, boxRect.top + depth, boxRect.right + depth, boxRect.bottom + depth)
            canvas.drawRoundRect(shadowRect, radius * 0.8f, radius * 0.8f, shadowPaint)

            boxDrawable!!.setBounds(boxRect.left.toInt(), boxRect.top.toInt(), boxRect.right.toInt(), boxRect.bottom.toInt())
            boxDrawable!!.draw(canvas)
        } else {
            val paint = if (onTarget) boxOnTargetPaint else boxPaint

            // 阴影
            val depth = cellSize * 0.05f
            val shadowRect = RectF(boxRect.left + depth, boxRect.top + depth, boxRect.right + depth, boxRect.bottom + depth)
            canvas.drawRoundRect(shadowRect, radius * 0.8f, radius * 0.8f, shadowPaint)

            canvas.drawRoundRect(boxRect, radius * 0.8f, radius * 0.8f, paint)

            // 箱子上的X标记
            val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (onTarget) Color.parseColor("#2E7D32") else {
                    if (isTech) Color.parseColor("#CC6A30") else Color.parseColor("#B5733A")
                }
                strokeWidth = 2.5f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            val mi = cellSize * 0.15f
            canvas.drawLine(boxRect.left + mi, boxRect.top + mi, boxRect.right - mi, boxRect.bottom - mi, markPaint)
            canvas.drawLine(boxRect.right - mi, boxRect.top + mi, boxRect.left + mi, boxRect.bottom - mi, markPaint)
        }

        // 箱子在目标点上时画绿色对勾
        if (onTarget) {
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                strokeWidth = 3f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val cx = (boxRect.left + boxRect.right) / 2
            val cy = (boxRect.top + boxRect.bottom) / 2
            val s = cellSize * 0.12f
            val path = Path().apply {
                moveTo(cx - s, cy)
                lineTo(cx - s * 0.2f, cy + s * 0.8f)
                lineTo(cx + s, cy - s * 0.6f)
            }
            canvas.drawPath(path, checkPaint)
        }
    }

    private fun drawPlayer(canvas: Canvas, rect: RectF) {
        val playerRect = RectF(rect)

        if (playerDrawable != null) {
            playerDrawable!!.setBounds(
                playerRect.left.toInt(), playerRect.top.toInt(),
                playerRect.right.toInt(), playerRect.bottom.toInt()
            )
            playerDrawable!!.draw(canvas)
        } else {
            // 绘制可爱的圆形角色
            val cx = (rect.left + rect.right) / 2
            val cy = (rect.top + rect.bottom) / 2
            val r = cellSize * 0.32f

            // 外圈光晕
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, r * 1.3f,
                    intArrayOf(Color.argb(60, playerPaint.color shr 16 and 0xFF, playerPaint.color shr 8 and 0xFF, playerPaint.color and 0xFF), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, r * 1.3f, glowPaint)

            // 主体
            canvas.drawCircle(cx, cy, r, playerPaint)

            // 高光
            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, 255, 255, 255)
            }
            canvas.drawCircle(cx - r * 0.2f, cy - r * 0.2f, r * 0.4f, highlightPaint)

            // 眼睛
            val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#333333") }
            val eyeR = r * 0.15f
            val eyeY = cy - r * 0.1f
            canvas.drawCircle(cx - r * 0.3f, eyeY, eyeR, eyePaint)
            canvas.drawCircle(cx + r * 0.3f, eyeY, eyeR, eyePaint)
            canvas.drawCircle(cx - r * 0.3f, eyeY, eyeR * 0.6f, pupilPaint)
            canvas.drawCircle(cx + r * 0.3f, eyeY, eyeR * 0.6f, pupilPaint)

            // 微笑
            val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#333333")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                strokeCap = Paint.Cap.ROUND
            }
            val smileRect = RectF(cx - r * 0.3f, cy, cx + r * 0.3f, cy + r * 0.4f)
            canvas.drawArc(smileRect, 0f, 180f, false, smilePaint)
        }
    }
}
