package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.util.Config

enum class Mode {
    NAV_DRAWER, FILTER_DRAWER, SELECTION, MAIN
}

enum class Action {
    LINK,
    SMS,
    PHONE,
    MAIL
}

enum class Event {
    TASK_LIST_CHANGED,
    SAVED_FILTER_ITEM_CLICK,
    FILTER_CHANGED,
    QUICK_FILTER_ITEM_CLICK,
    ADDED_SAVED_FILTER,
    RESUME,
    IMPORTED_FILTERS,
    FONT_SIZE_CHANGED,
    UPDATE_PENDING_CHANGES,
    CLEAR_FILTER,
    RENAMED_SAVED_FILTER
}


fun Simpletask.updateUiForEvent(event: Event) {
    val tag = "Event"
    log.debug(tag, "update UI for event ${event.name}")
    runOnUiThread {
        when (event) {
            Event.SAVED_FILTER_ITEM_CLICK -> {
                updateTaskList(Config.mainQuery) {
                    updateFilterBar()
                    updateQuickFilterDrawer()
                }
            }
            Event.QUICK_FILTER_ITEM_CLICK,
            Event.CLEAR_FILTER -> {
                updateTaskList(Config.mainQuery) {
                    updateFilterBar()
                    updateQuickFilterDrawer()
                }

            }
            Event.ADDED_SAVED_FILTER,
            Event.RENAMED_SAVED_FILTER -> {
                updateSavedFilterDrawer()
            }
            Event.TASK_LIST_CHANGED -> {
                updateTaskList(Config.mainQuery) {
                    updateFilterBar()
                    updateQuickFilterDrawer()
                }
            }
            Event.FILTER_CHANGED -> {
                updateTaskList(Config.mainQuery) {
                    updateFilterBar()
                    updateQuickFilterDrawer()
                }
            }
            Event.RESUME -> {
                updateFilterBar()
                updateSavedFilterDrawer()
                updateQuickFilterDrawer()
                updateConnectivityIndicator()
            }
            Event.IMPORTED_FILTERS -> {
                updateSavedFilterDrawer()
            }
            Event.FONT_SIZE_CHANGED -> {
                updateTaskList(Config.mainQuery) {
                    updateFilterBar()
                    updateQuickFilterDrawer()
                }
            }
            Event.UPDATE_PENDING_CHANGES -> {
                updateConnectivityIndicator()
            }
        }
    }
}
