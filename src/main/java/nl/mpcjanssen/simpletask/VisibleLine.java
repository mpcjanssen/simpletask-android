package nl.mpcjanssen.simpletask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created by Mark on 2015-08-05.
 */
public class VisibleLine {
    Task task;
    String title = "";
    public boolean header = false;

    public VisibleLine(@NonNull String title) {
        this.title = title;
        this.header = true;
    }

    public VisibleLine(@NonNull Task task) {
        this.task = task;
        this.header = false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        VisibleLine other = (VisibleLine) obj;
        return other.header == this.header && this.task.equals(other.task);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        int headerHash = header ? 1231 : 1237;
        result = prime * result + headerHash;
        result = prime * result + task.hashCode();
        return result;
    }
}