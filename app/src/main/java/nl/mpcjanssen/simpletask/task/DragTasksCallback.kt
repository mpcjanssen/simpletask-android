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

    // Called when the line we're dragging is being swapped with another line
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition

        if (taskAdapter.canMoveVisibleLine(from, to)) {
            // We don't really save the move to the todo.txt file here.  Instead
            // we update the current state of what the move is that the user is
            // doing in total.
            updateCurrentMove(from, to)
            taskAdapter.visuallyMoveLine(from, to)
            return true
        } else {
            return false
        }
    }

    // Called when the dragging is done (We should now clear any decoractions we
    // added to the view that was being dragged, hence the name.  We mainly use
    // this callback to persist the move to the todo.txt file now that it's
    // done.)
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
        // Lower indices are higher up in the list.  We look at the current
        // swapping of two items:  If we come from higher up in the list, we
        // want the dragging item to be placed below the other item.  If we
        // come from lower down, then above.
        val isMoveBelow = nowFrom < nowTo

        val toTask = taskAdapter.getMovableTaskAt(nowTo)
        val toLineIndex = nowTo

        val oldCurrentMove = currentMove
        if (oldCurrentMove == null) {
            // If `currentMove` wasn't set before, the "from" task is the task
            // we're currently dragging.
            // (Easy refactoring idea: Actually, we don't need to save the
            // "from" task because it's always the task that is currently being
            // dragged.  I.e. we can drop both "from" attributes from the
            // LineMove class and just compute them in `clearView` the way
            // they're computed here / in `onMove`.  I think this would also
            // allow us to get rid of the if statement here.)
            val fromLineIndex = nowFrom
            val fromTask = taskAdapter.getMovableTaskAt(nowFrom)

            currentMove = LineMove(fromLineIndex, toLineIndex, fromTask, toTask, isMoveBelow)
        } else {
            val fromLineIndex = oldCurrentMove.fromLineIndex
            val fromTask = oldCurrentMove.fromTask

            currentMove = LineMove(fromLineIndex, toLineIndex, fromTask, toTask, isMoveBelow)
        }
    }
}
