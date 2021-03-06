package com.pechuro.cashdebts.ui.fragment.localdebtlist.adapter

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.ui.base.BaseItemTouchCallback
import com.pechuro.cashdebts.ui.base.BaseViewHolder
import com.pechuro.cashdebts.ui.utils.extensions.px
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LocalDebtItemSwipeCallback @Inject constructor() :
    BaseItemTouchCallback<LocalDebtItemSwipeCallback.SwipeAction>(
        0,
        ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
    ) {
    private val _actionEmitter = PublishSubject.create<SwipeAction>()

    override val actionEmitter: Observable<SwipeAction>
        get() = _actionEmitter

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        when (direction) {
            ItemTouchHelper.RIGHT -> _actionEmitter.onNext(SwipeAction.Delete(viewHolder.adapterPosition))
            ItemTouchHelper.LEFT -> _actionEmitter.onNext(SwipeAction.Edit(viewHolder.adapterPosition))
        }
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ) = if ((viewHolder as? BaseViewHolder<*>)?.isSwipeable == false) {
        0
    } else {
        super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val cornersRadius = if (isCurrentlyActive) 8.px.toFloat() else 0F
        (viewHolder.itemView as? CardView)?.radius = cornersRadius

        val context = recyclerView.context
        var drawingDx = dX

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 10.px

        val icon: Drawable
        val backgroundColor: ColorDrawable
        when {
            dX >= 0 -> {
                icon = recyclerView.context.resources.getDrawable(
                    R.drawable.ic_delete,
                    context.theme
                )
                backgroundColor =
                    ColorDrawable(ContextCompat.getColor(context, R.color.action_delete))
            }
            dX < 0 -> {
                icon = recyclerView.context.resources.getDrawable(
                    R.drawable.ic_edit_white,
                    context.theme
                )
                backgroundColor =
                    ColorDrawable(
                        ContextCompat.getColor(
                            context,
                            R.color.action_edit
                        )
                    )
                drawingDx /= 4
            }
            else -> throw IllegalArgumentException()
        }

        val iconTop = itemView.top + ((itemView.height - icon.intrinsicHeight) / 2)
        val iconBottom = iconTop + icon.intrinsicHeight
        val iconPadding = 16.px

        when {
            dX > 0 -> {
                val iconLeft = itemView.left + iconPadding
                val iconRight = iconLeft + icon.intrinsicWidth
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                backgroundColor.setBounds(
                    itemView.left, itemView.top,
                    itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom
                )
            }
            dX < 0 -> {
                val iconLeft = itemView.right - iconPadding - icon.intrinsicWidth
                val iconRight = itemView.right - iconPadding
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                backgroundColor.setBounds(
                    itemView.right + dX.toInt() - backgroundCornerOffset,
                    itemView.top, itemView.right, itemView.bottom
                )
            }
            else -> backgroundColor.setBounds(0, 0, 0, 0)
        }

        backgroundColor.draw(canvas)
        icon.draw(canvas)

        super.onChildDraw(
            canvas,
            recyclerView,
            viewHolder,
            drawingDx,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    sealed class SwipeAction : BaseItemTouchCallback.TouchActions() {
        class Delete(val position: Int) : SwipeAction()
        class Edit(val position: Int) : SwipeAction()
    }
}