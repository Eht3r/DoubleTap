package com.example.doubletap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.currentCoroutineContext

interface SwipeControllerActions {
    fun onLeftClicked(position: Int)
    fun onRightClicked(position: Int)
}

class SwipeController(private val context: Context, private val actions:
        SwipeControllerActions): ItemTouchHelper.Callback() {

    private val paint = Paint()
    private var swipeBack = false
    private var buttonShowedState = ButtonsState.GONE
    private var buttonInstance: RectF? = null

    // 스와이프 방향 설정
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT)
    }

    // 스와이프 이벤트 발생 시 호출
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // 스와이프 후의 동작 정의
        // 사용 안함, ItemTouchHelper 사용시 필수 오버라이딩
    }

    // 메뉴 그리기
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ACTION_STATE_SWIPE) {
            val itemView = viewHolder.itemView

            val buttonWWidthWithoutPadding = 300f // 각 버튼의 너비 설정
            val buttonWidth = buttonWWidthWithoutPadding + 20 // 버튼 너비 설정

            if (dX < 0) {
                // 삭제 버튼
                paint.color = Color.RED
                val deleteButton = RectF(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(deleteButton, paint)
                drawText("삭제", c, deleteButton, paint)

                // 공유 버튼
                paint.color = Color.BLUE
                val shareButton = RectF(
                    itemView.right + dX + buttonWidth,
                    itemView.top.toFloat(),
                    itemView.right + dX + 2 * buttonWidth,
                    itemView.bottom.toFloat()
                )
                c.drawRect(shareButton, paint)
                drawText("공유", c, shareButton, paint)

                buttonInstance = if (dX < -buttonWWidthWithoutPadding) deleteButton else shareButton
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    // 버튼의 텍스트 그리기
    private fun drawText(
        text: String,
        c: Canvas,
        button: RectF,
        p: Paint
    ) {
        val textSize = 40f
        p.color = Color.WHITE
        p.isAntiAlias = true
        p.textSize = textSize

        val textWidth: Float = p.measureText(text)
        c.drawText(text, button.centerX() - textWidth / 2, button.centerY() + textSize / 2, p)
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (buttonInstance != null && isCurrentlyActive) {
            setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            super.onChildDrawOver(
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack =
                event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP

            if (swipeBack) {
                if (buttonInstance != null &&
                    buttonInstance!!.contains(event.x, event.y)
                ) {
                    if (dX < -150)
                        actions.onRightClicked(viewHolder!!.adapterPosition)
                    else actions.onLeftClicked(viewHolder!!.adapterPosition)
                }
                setItemsClickable(recyclerView, true)
                swipeBack = false
            }
            false
        }
    }
    private fun setItemsClickable(recyclerView: RecyclerView, isClickable: Boolean) {
        for (i in 0 until recyclerView.childCount) {
            recyclerView.getChildAt(i).isClickable = isClickable
        }
    }

    enum class ButtonsState {
        GONE,
        LEFT_VISIBLE,
        RIGHT_VISIBLE
    }
}