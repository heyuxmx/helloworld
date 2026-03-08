package com.heyu.zhudeapp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SudokuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cells = IntArray(81)
    private var given = BooleanArray(81)
    private var errors = BooleanArray(81)
    private var selected = -1
    private var cellSize = 0f
    private var drafts = Array(81) { emptySet<Int>() }

    private val density = context.resources.displayMetrics.density

    // Cute color palette
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#FFD6E8"); style = Paint.Style.FILL
    }
    private val sameNumPaint = Paint().apply {
        color = Color.parseColor("#D6E8FF"); style = Paint.Style.FILL
    }
    private val relatedPaint = Paint().apply {
        color = Color.parseColor("#FFF0F5"); style = Paint.Style.FILL
    }
    private val errorBgPaint = Paint().apply {
        color = Color.parseColor("#FFE0E0"); style = Paint.Style.FILL
    }
    private val thickLinePaint = Paint().apply {
        color = Color.parseColor("#C0A0B0"); strokeWidth = 3f * density
        style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val thinLinePaint = Paint().apply {
        color = Color.parseColor("#E0D0D8"); strokeWidth = 1f * density
        style = Paint.Style.STROKE; isAntiAlias = true
    }
    private val givenTextPaint = Paint().apply {
        color = Color.parseColor("#555555"); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val userTextPaint = Paint().apply {
        color = Color.parseColor("#2EAA5A"); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val errorTextPaint = Paint().apply {
        color = Color.parseColor("#E03040"); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    private val draftTextPaint = Paint().apply {
        color = Color.parseColor("#999999"); textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var onCellTapped: ((Int) -> Unit)? = null

    fun setBoard(
        cells: IntArray, given: BooleanArray, errors: BooleanArray,
        selectedCell: Int, drafts: Array<Set<Int>> = Array(81) { emptySet() }
    ) {
        this.cells = cells.copyOf()
        this.given = given.copyOf()
        this.errors = errors.copyOf()
        this.selected = selectedCell
        this.drafts = drafts.map { it.toSet() }.toTypedArray()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = minOf(w, if (h > 0) h else w)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        cellSize = width / 9f
        val textSize = cellSize * 0.58f
        givenTextPaint.textSize = textSize
        userTextPaint.textSize = textSize
        errorTextPaint.textSize = textSize
        draftTextPaint.textSize = cellSize * 0.28f

        val selectedNum = if (selected in 0..80 && cells[selected] != 0) cells[selected] else 0

        // Cell backgrounds
        for (i in 0..80) {
            val r = i / 9
            val c = i % 9
            val x = c * cellSize
            val y = r * cellSize

            val paint = when {
                i == selected -> selectedPaint
                errors[i] -> errorBgPaint
                selectedNum != 0 && cells[i] == selectedNum -> sameNumPaint
                selected >= 0 && isRelated(i, selected) -> relatedPaint
                else -> null
            }
            paint?.let { canvas.drawRect(x, y, x + cellSize, y + cellSize, it) }
        }

        // Grid lines
        for (i in 0..9) {
            val pos = i * cellSize
            val paint = if (i % 3 == 0) thickLinePaint else thinLinePaint
            canvas.drawLine(pos, 0f, pos, width.toFloat(), paint)
            canvas.drawLine(0f, pos, width.toFloat(), pos, paint)
        }

        // Numbers
        for (i in 0..80) {
            val num = cells[i]
            if (num != 0) {
                val r = i / 9
                val c = i % 9
                val x = c * cellSize + cellSize / 2
                val y = r * cellSize + cellSize / 2

                val paint = when {
                    errors[i] -> errorTextPaint
                    given[i] -> givenTextPaint
                    else -> userTextPaint
                }
                val textY = y - (paint.descent() + paint.ascent()) / 2
                canvas.drawText(num.toString(), x, textY, paint)
            } else {
                // Draw draft numbers as a 3x3 mini-grid
                val ds = drafts[i]
                if (ds.isNotEmpty()) {
                    val row = i / 9
                    val col = i % 9
                    val cellX = col * cellSize
                    val cellY = row * cellSize

                    for (n in ds) {
                        val dr = (n - 1) / 3  // 0, 1, 2
                        val dc = (n - 1) % 3  // 0, 1, 2
                        val nx = cellX + cellSize * (dc * 2 + 1) / 6
                        val ny = cellY + cellSize * (dr * 2 + 1) / 6
                        val ty = ny - (draftTextPaint.descent() + draftTextPaint.ascent()) / 2
                        canvas.drawText(n.toString(), nx, ty, draftTextPaint)
                    }
                }
            }
        }
    }

    private fun isRelated(i: Int, sel: Int): Boolean {
        val r1 = i / 9; val c1 = i % 9
        val r2 = sel / 9; val c2 = sel % 9
        return r1 == r2 || c1 == c2 || (r1 / 3 == r2 / 3 && c1 / 3 == c2 / 3)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val c = (event.x / cellSize).toInt().coerceIn(0, 8)
            val r = (event.y / cellSize).toInt().coerceIn(0, 8)
            onCellTapped?.invoke(r * 9 + c)
            performClick()
            return true
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
