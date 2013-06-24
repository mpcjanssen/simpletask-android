package nl.mpcjanssen.simpletask.remote;

public enum DropboxFileStatus {
    INITIALIZED,
    STARTED,
    FOUND,
    NOT_FOUND,
    NOT_CHANGED,
    CONFLICT,
    SUCCESS,
    ERROR
}
