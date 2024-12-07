package com.example.doubletap

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class FileListAdapterDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildAdapterPosition(view)
        val offsetTop = 16
        val offset = 4

        if (position == 0) {
            outRect.top = offsetTop
        } else {
            outRect.top = offset
        }
    }
}