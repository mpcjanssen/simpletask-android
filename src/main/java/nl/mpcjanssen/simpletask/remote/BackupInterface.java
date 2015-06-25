package nl.mpcjanssen.simpletask.remote;

/**
 * Interface definition of the storage backend used.
 *
 * Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.
 */
public interface BackupInterface {

    void backup (String name, String contents);
}
