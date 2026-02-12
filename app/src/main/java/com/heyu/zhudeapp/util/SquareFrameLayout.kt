package com.heyu.zhudeapp.util

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class SquareFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force square: use the width measurement for the height as well
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
