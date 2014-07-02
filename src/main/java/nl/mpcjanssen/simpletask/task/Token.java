package nl.mpcjanssen.simpletask.task;

/**
 * Created Mark Janssen
 */
class Token {
    static final String WHITE_SPACE = "WS";
    static final String LIST = "LIST";
    static final String TAG = "TAG";
    static final String COMPLETED = "X";
    static final String COMPLETED_DATE = "COMPLDATE";
    static final String CREATION_DATE = "COMPLDATE";
    static final String TEXT = "TEXT";
    public String type;
    public String value;
    public Object objValue;
    public Token (String type, String value, Object objValue) {
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

        if (!type.equals(token.type)) return false;
        if (!value.equals(token.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}