package nl.mpcjanssen.simpletask;


import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

/**
 * Generates entities and DAOs for the example project DaoExample.
 *
 * Run it as a Java application (not Android).
 *
 */


/**
 * Two schemas:
 * 1. application storage, Log and TodoList backups
 * 2. TodoList, Todo items en Archived items
 */


public class SimpletaskDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema appSchema = new Schema(1011, "nl.mpcjanssen.simpletask.dao.app");
        Schema todoSchema = new Schema(4, "nl.mpcjanssen.simpletask.dao.todo");
        addEntities(appSchema);
        addTodoEntities(todoSchema);
        new DaoGenerator().generateAll(appSchema, "src/main/java");
    }

    private static void addTodoEntities(Schema todoSchema) {
        Entity todo = todoSchema.addEntity("TodoItem");
        todo.addLongProperty("line").notNull().primaryKey();
        todo.addStringProperty("contents").notNull();
        todo.addBooleanProperty("selected").notNull();

        Entity archive = todoSchema.addEntity("ArchiveItem");
        archive.addStringProperty("json").notNull();
    }

    private static void addEntities(Schema appSchema) {
        backupSchema(appSchema);
        logSchema(appSchema);
    }


    private static void logSchema(Schema appSchema) {
        Entity log = appSchema.addEntity("LogItem");
        log.addDateProperty("timestamp").notNull();
        log.addStringProperty("severity").notNull();
        log.addStringProperty("tag").notNull();
        log.addStringProperty("message").notNull();
        log.addStringProperty("exception").notNull();
    }

    private static void backupSchema(Schema appSchema) {
        Entity entry = appSchema.addEntity("TodoBackup");
        entry.addStringProperty("contents").notNull().primaryKey();
        entry.addDateProperty("date").notNull();
    }

}