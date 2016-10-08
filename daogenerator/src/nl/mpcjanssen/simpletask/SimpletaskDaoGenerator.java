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


public class SimpletaskDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema backupSchema = new Schema(1013, "nl.mpcjanssen.simpletask.dao.genbackup");
        Schema todoSchema = new Schema(1, "nl.mpcjanssen.simpletask.dao.gentodo");
        Schema logSchema = new Schema(1, "nl.mpcjanssen.simpletask.dao.genlog");

        backupSchema(backupSchema);
        todoSchema(todoSchema);
        logSchema(logSchema);
        new DaoGenerator().generateAll(backupSchema, "src/main/java");
        new DaoGenerator().generateAll(logSchema, "src/main/java");
        new DaoGenerator().generateAll(todoSchema, "src/main/java");
    }



    private static void logSchema(Schema schema) {
        Entity log = schema.addEntity("LogItem");
        log.addIdProperty().primaryKey().autoincrement();
        log.addDateProperty("timestamp").notNull();
        log.addStringProperty("severity").notNull();
        log.addStringProperty("tag").notNull();
        log.addStringProperty("message").notNull();
        log.addStringProperty("exception").notNull();
    }

    private static void backupSchema(Schema schema) {
        Entity entry = schema.addEntity("TodoFile");
        entry.addStringProperty("contents").notNull().primaryKey();
        entry.addStringProperty("name").notNull();
        entry.addDateProperty("date").notNull();
    }

    private static void todoSchema(Schema schema) {
        Entity entry = schema.addEntity("TodoItem");
        entry.addLongProperty("line").notNull().primaryKey();
        entry.addStringProperty("task").customType("nl.mpcjanssen.simpletask.task.Task", "nl.mpcjanssen.simpletask.dao.TaskPropertyConverter");
        entry.addBooleanProperty("selected").notNull();
    }
}