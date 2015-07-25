package nl.mpcjanssen.simpletask.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ListenerList<L> {
    @NonNull
    private List<L> listenerList = new ArrayList<>();

    public interface FireHandler<L> {
        void fireEvent(L listener);
    }

    public void add(L listener) {
        listenerList.add(listener);
    }

    public void fireEvent(@NonNull FireHandler<L> fireHandler) {
        List<L> copy = new ArrayList<>(listenerList);
        for (L l : copy) {
            fireHandler.fireEvent(l);
        }
    }

    public void remove(L listener) {
        listenerList.remove(listener);
    }

    @NonNull
    public List<L> getListenerList() {
        return listenerList;
    }
}
