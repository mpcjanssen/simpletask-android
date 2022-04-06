package nl.mpcjanssen.simpletask.task

import android.util.Log;

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragTasksCallback(val taskAdapter: TaskAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder.itemViewType == 1) { // Task
            val fromIndex = viewHolder.bindingAdapterPosition
            val canMoveUp = taskAdapter.canMoveVisibleLine(fromIndex, fromIndex - 1)
            val canMoveDown = taskAdapter.canMoveVisibleLine(fromIndex, fromIndex + 1)

            // If e.g. the item can only be moved down, we also allow dragging
            // in the UP direction.  This allows the item to be moved back to
            // its original position, because to do so, you need to drag it
            // slightly further up than it was previously.
            if (canMoveUp || canMoveDown) {
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }
        }
        return 0
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        if (taskAdapter.canMoveVisibleLine(from, to)) {
            taskAdapter.moveVisibleLine(from, to)
            return true
        } else {
            return false
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        taskAdapter.persistVisibleLineMove()
    }

    override fun onSwiped(recyclerView: RecyclerView.ViewHolder, p1: Int): Unit {
        throw IllegalStateException("Swiping is not enabled for the ItemTouchHelper, so it shouldn't ever call onSwiped");
    }
}
