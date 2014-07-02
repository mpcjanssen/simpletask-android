package nl.mpcjanssen.simpletask.task;

/**
 * Created Mark Janssen
 */
public class Token {
    public static final int WHITE_SPACE =      0x1;
    public static final int LIST =             0x1<<1;
    public static final int TAG =              0x1<<2;
    public static final int COMPLETED =        0x1<<3;
    public static final int COMPLETED_DATE =   0x1<<4;
    public static final int CREATION_DATE =    0x1<<5;
    public static final int TEXT =             0x1<<6;
    public int type;
    public String value;
    public Object objValue;
    public Token (int type, String value, Object objValue) {
        this.type = type;
        this.value = value;
        this.objValue = objValue;
    }

    @Override
    public String toString() {
        return type + ":'" + value + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (!(type==token.type)) return false;
        if (!value.equals(token.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + value.hashCode();
        return result;
    }
}