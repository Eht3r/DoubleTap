package com.example.doubletap

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

interface SwipeControllerActions {
    fun onLeftClicked(position: Int)
    fun onRightClicked(position: Int)
}

class SwipeController(
    private val actions:
    SwipeControllerActions
): ItemTouchHelper.Callback() {

    private val paint = Paint()
    private var swipeBack = false
    private var buttonShowedState = ButtonsState.GONE
    private var buttonInstance: RectF? = null

    // 스와이프 방향 설정
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.adapterPosition
        val item = (recyclerView.adapter as MainAdapter).items[position]

        // 폴더 추가와 파일 추가 아이템에는 스와이프가 동작되면 안됨
        return if (item.name == "폴더 추가") {
            makeMovementFlags(0, 0)
        }else if (viewHolder is FileListAdapter.AddViewHolder) {
            makeMovementFlags(0, 0)
        }
        else {
            makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
        }
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

    // 메뉴 그리기 (항목의 배경을 그림)
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
            val itemWidth = itemView.width
            val maxSwipeDistance = itemWidth / 2

            val limitedDx = when {
                dX > maxSwipeDistance -> maxSwipeDistance.toFloat()
                dX < -maxSwipeDistance -> -maxSwipeDistance.toFloat()
                else -> dX
            }

            if (limitedDx < 0) { // 왼쪽으로 스와이프
                // 삭제 버튼
                paint.color = Color.RED
                val deleteButton = RectF(
                    itemView.right + limitedDx,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(deleteButton, paint)
                drawText("삭제", c, deleteButton, paint)
                buttonInstance = deleteButton
            } else if(limitedDx > 0) { // 오른쪽으로 스와이프
                // 공유 버튼
                paint.color = Color.BLUE
                val shareButton = RectF(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.left + limitedDx,
                    itemView.bottom.toFloat()
                )
                c.drawRect(shareButton, paint)
                drawText("공유", c, shareButton, paint)
                buttonInstance = shareButton
            }

            // 요소가 사라지지 않도록 trainslateX 설정
            // 스와이프를 요소의 절반 크기 정도만 진행하도록 제한
            itemView.translationX = limitedDx

            //  반대 방향으로 스와이프 시 버튼 초기화
            if ((buttonShowedState == ButtonsState.RIGHT_VISIBLE && dX > -maxSwipeDistance) ||
                (buttonShowedState == ButtonsState.LEFT_VISIBLE && dX < -maxSwipeDistance)
                ) {
                buttonShowedState = ButtonsState.GONE
                buttonInstance = null
                itemView.translationX = 0f // 원래 워치로 복귀
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
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

    // 그려진 매뉴 위에 추가적인 그래픽 요소 추가
    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemWidth = itemView.width
        val maxSwipeDistance = itemWidth /2

        if (buttonInstance != null && isCurrentlyActive) {
            if ((buttonShowedState == ButtonsState.RIGHT_VISIBLE && dX < -maxSwipeDistance) ||
                (buttonShowedState == ButtonsState.LEFT_VISIBLE && dX > -maxSwipeDistance)
            ) {
                // 반대 방향 스와이프 시 포기화
                buttonShowedState = ButtonsState.GONE
                buttonInstance = null

                recyclerView.adapter?.notifyItemChanged(viewHolder!!.adapterPosition)
            } else {
                setTouchListener(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
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

    // 메뉴 클릭 처리
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
                if (buttonInstance != null && buttonInstance!!.contains(event.x, event.y)) {
                    if (abs(dX) < 50) { // 스와이프가 아닌 경우에만 클릭 처리
                        if (dX < 0) actions.onRightClicked(viewHolder!!.adapterPosition)
                        else actions.onLeftClicked(viewHolder!!.adapterPosition)
                    }
                } else { // 스와이프시 원상 복귀
                    buttonShowedState = ButtonsState.GONE
                    buttonInstance = null
                }
                setItemsClickable(recyclerView, true)
                swipeBack = false
            } else if (event.action == MotionEvent.ACTION_DOWN) {
                if (buttonShowedState != ButtonsState.GONE) {
                    buttonShowedState = ButtonsState.GONE
                    buttonInstance = null
                    recyclerView.adapter?.notifyItemChanged(viewHolder!!.adapterPosition)
                }
            }
            false
        }
    }
    private fun setItemsClickable(recyclerView: RecyclerView, isClickable: Boolean) {
        for (i in 0 until recyclerView.childCount) {
            recyclerView.getChildAt(i).isClickable = isClickable
        }
    }

    // 버튼 상태 지정
    enum class ButtonsState {
        GONE,
        LEFT_VISIBLE,
        RIGHT_VISIBLE
    }
}