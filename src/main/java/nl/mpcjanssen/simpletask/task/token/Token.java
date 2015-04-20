package nl.mpcjanssen.simpletask.task.token;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created Mark Janssen
 */
abstract public class Token {
    public static final int WHITE_SPACE = 0x1;
    public static final int LIST = 0x1 << 1;
    public static final int TTAG = 0x1 << 2;
    public static final int COMPLETED = 0x1 << 3;
    public static final int COMPLETED_DATE = 0x1 << 4;
    public static final int CREATION_DATE = 0x1 << 5;
    public static final int TEXT = 0x1 << 6;
    public static final int PRIO = 0x1 << 7;
    public static final int THRESHOLD_DATE = 0x1 << 8;
    public static final int DUE_DATE = 0x1 << 9;
    public static final int HIDDEN = 0x1 << 10;
    public static final int SHOW_ALL = ~0x0;
    public int type;
    public String value;

    public Token(int type, String value) {
        this.type = type;
        this.value = value;
    }

    @NotNull
    @Override
    public String toString() {
        return type + ":'" + value + "'";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (!(type == token.type)) return false;
        return value.equals(token.value);

    }


    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + value.hashCode();
        return result;
    }
}