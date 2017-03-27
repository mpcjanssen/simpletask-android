package com.buildware.widget.indeterm;

import android.widget.Checkable;

/**
 * Extension to Checkable interface with addition "indeterminate" state
 * represented by <code>getState()</code>. Value meanings:
 *   null = indeterminate state
 *   true = checked state
 *   false = unchecked state
 */
public interface IndeterminateCheckable extends Checkable {
    void setIndeterminateUsed(Boolean bool);
    void setState(Boolean state);
    Boolean getState();
}
