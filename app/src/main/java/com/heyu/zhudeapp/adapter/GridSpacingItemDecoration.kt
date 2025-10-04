package com.heyu.zhudeapp.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 一个为 RecyclerView 的 GridLayoutManager 添加均匀间距的 ItemDecoration。
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,      // 网格的列数
    private val spacing: Int,        // 间距大小 (单位: px)
    private val includeEdge: Boolean   // 是否在网格边缘也包含间距
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item 的位置
        val column = position % spanCount // item 所在的列

        if (includeEdge) {
            // 算法1：包含边缘间距的计算方式
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) { // 顶部边缘
                outRect.top = spacing
            }
            outRect.bottom = spacing // item 底部
        } else {
            // 算法2：不包含边缘间距的计算方式
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing // item 顶部
            }
        }
    }
}
