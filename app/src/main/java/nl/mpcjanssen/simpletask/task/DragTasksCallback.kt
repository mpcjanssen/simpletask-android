package nl.mpcjanssen.simpletask.task

import android.util.Log;

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class LineMove(val fromLineIndex: Int,
        val toLineIndex: Int,
        val fromTask: Task,
        val toTask: Task,
        val isMoveBelow: Boolean) {}

class DragTasksCallback(val taskAdapter: TaskAdapter) : ItemTouchHelper.Callback() {
    var currentMove: LineMove? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder.itemViewType == 1) { // Task
            val fromIndex = viewHolder.bindingAdapterPosition
            val canMove = taskAdapter.canMoveLineUpOrDown(fromIndex)

            // If e.g. the item could really only be moved down, we also allow
            // dragging in the UP direction.  This allows the item to be moved
            // back to its original position, because to do so, you need to drag
            // it slightly further up than it was previously.
            if (canMove) {
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }
        }
        return 0
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        if (taskAdapter.canMoveVisibleLine(from, to)) {
            updateCurrentMove(from, to)
            taskAdapter.visuallyMoveLine(from, to)
            return true
        } else {
            return false
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        var finalMove = currentMove

        if (finalMove != null && finalMove.fromLineIndex != finalMove.toLineIndex) {
            taskAdapter.persistLineMove(finalMove.fromTask, finalMove.toTask, finalMove.isMoveBelow)
        }

        currentMove = null
    }

    override fun onSwiped(recyclerView: RecyclerView.ViewHolder, p1: Int): Unit {
        throw IllegalStateException("Swiping is not enabled for the ItemTouchHelper, so it shouldn't ever call onSwiped");
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    private fun updateCurrentMove(nowFrom: Int, nowTo: Int) {
        val toTask = taskAdapter.getMovableTaskAt(nowTo)
        val toLineIndex = nowTo

        val oldCurrentMove = currentMove
        if (oldCurrentMove == null) {
            val fromLineIndex = nowFrom
            val fromTask = taskAdapter.getMovableTaskAt(nowFrom)
            val isMoveBelow = nowFrom < nowTo
            currentMove = LineMove(fromLineIndex, toLineIndex, fromTask, toTask, isMoveBelow)
        } else {
            val fromLineIndex = oldCurrentMove.fromLineIndex
            val fromTask = oldCurrentMove.fromTask
            val isMoveBelow = nowFrom < nowTo
            currentMove = LineMove(fromLineIndex, toLineIndex, fromTask, toTask, isMoveBelow)
        }
    }
}
