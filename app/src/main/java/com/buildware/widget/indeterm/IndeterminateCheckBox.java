package com.buildware.widget.indeterm;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.ViewDebug;
import nl.mpcjanssen.simpletask.R;

/**
 * A CheckBox with additional 3rd "indeterminate" state.
 * By default it is in "determinate" (checked or unchecked) state.
 * @author Svetlozar Kostadinov (sevarbg@gmail.com)
 */
public class IndeterminateCheckBox extends AppCompatCheckBox
        implements IndeterminateCheckable {

    private static final int[] INDETERMINATE_STATE_SET = {
            R.attr.state_indeterminate
    };

    private boolean mIndeterminate;
    private boolean mIndeterminateUsed;
    private boolean mBroadcasting;
    private OnStateChangedListener mOnStateChangedListener;

    /**
     * Interface definition for a callback to be invoked when the checked state changed.
     */
    public interface OnStateChangedListener {
        /**
         * Called when the indeterminate state has changed.
         *
         * @param checkBox The checkbox view whose state has changed.
         * @param newState The new state of checkBox. Value meanings:
         *              null = indeterminate state
         *              true = checked state
         *              false = unchecked state
         */
        void onStateChanged(IndeterminateCheckBox checkBox, @Nullable Boolean newState);
    }

    public IndeterminateCheckBox(Context context) {
        this(context, null);
    }

    public IndeterminateCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkboxStyle);
    }

    public IndeterminateCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (Build.VERSION.SDK_INT >= 23) {
            setButtonDrawable(R.drawable.btn_checkmark);
        } else {
            //setSupportButtonTintList(ContextCompat.getColorStateList(context, R.color.control_checkable_material));
            setButtonDrawable(Utils.tintDrawable(this, R.drawable.btn_checkmark));
        }
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IndeterminateCheckable);
        // Read the XML attributes
        final boolean indeterminate = a.getBoolean(
                R.styleable.IndeterminateCheckable_indeterminate, false);
        if (indeterminate) {
            setIndeterminate(true);
        }
        a.recycle();

    }

    @Override
    public void toggle() {
        if (mIndeterminate) {
            setChecked(true);
            return;
        }
        if (getState()) {
            setChecked(false);
            return;
        }
        if (!getState() && mIndeterminateUsed) {
            setIndeterminate(true);
            return;
        }
        if (!getState()) {
            setChecked(true);
        }
    }

    public void setChecked(boolean checked) {
        final boolean checkedChanged = isChecked() != checked;
        super.setChecked(checked);
        final boolean wasIndeterminate = isIndeterminate();
        setIndeterminateImpl(false);
        if (wasIndeterminate || checkedChanged) {
            notifyStateListener();
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        final boolean indeterminateChanged = isIndeterminate() != indeterminate;
        setIndeterminateImpl(indeterminate);
        if (indeterminateChanged) {
            notifyStateListener();
        }
    }

    private void setIndeterminateImpl(boolean indeterminate) {
        if (mIndeterminate != indeterminate) {
            mIndeterminate = indeterminate;
            refreshDrawableState();
            /*notifyViewAccessibilityStateChangedIfNeeded(
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED); */
        }
    }

    @ViewDebug.ExportedProperty
    public boolean isIndeterminate() {
        return mIndeterminate;
    }

    @ViewDebug.ExportedProperty
    public Boolean getState() {
        return mIndeterminate ? null : isChecked();
    }

    @Override
    public void setIndeterminateUsed(Boolean bool) {
        mIndeterminateUsed = bool;
    }

    @Override
    public void setState(Boolean state) {
        if (state != null) {
            setChecked(state);
        } else {
            setIndeterminate(true);
        }
    }

    /**
     * Register a callback to be invoked when the indeterminate or checked state changes.
     * The standard <code>OnCheckedChangedListener</code> will still be called prior to
     * OnStateChangedListener.
     *
     * @param listener the callback to call on indeterminate or checked state change
     */
    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return IndeterminateCheckBox.class.getName();
    }

    private void notifyStateListener() {
        // Avoid infinite recursions if state is changed from a listener
        if (mBroadcasting) {
            return;
        }

        mBroadcasting = true;
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(this, getState());
        }
        mBroadcasting = false;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (getState() == null) {
            mergeDrawableStates(drawableState, INDETERMINATE_STATE_SET);
        }
        return drawableState;
    }

    static class SavedState extends BaseSavedState {
        boolean indeterminate;

        /**
         * Constructor called from {@link IndeterminateCheckBox#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            indeterminate = (boolean) in.readValue(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(indeterminate);
        }

        @Override
        public String toString() {
            return "IndeterminateCheckBox.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " indeterminate=" + indeterminate + "}";
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
