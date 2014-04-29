package nl.mpcjanssen.simpletask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class ApplicationIntent extends Intent {
    public ApplicationIntent(Context applicationContext, Class <?> targetClass, String action) {
        super(action);
        this.setClass(applicationContext, targetClass);
    }
}
