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
        Schema schema = new Schema(1011, "nl.mpcjanssen.simpletask.dao.gen");

        addEntities(schema);
        new DaoGenerator().generateAll(schema, "src/main/java");
    }

    private static void addEntities(Schema schema) {
        backupSchema(schema);
        logSchema(schema);
        todoModificationLog(schema);
    }

    private static void todoModificationLog(Schema schema) {
        Entity entity = schema.addEntity("TodoModificationItem");
        entity.addIdProperty().autoincrement();
        entity.addDateProperty("timestamp").notNull();
        entity.addStringProperty("file").notNull();
        entity.addStringProperty("action").notNull();
        entity.addStringProperty("from");
        entity.addStringProperty("to");
    }

    private static void logSchema(Schema schema) {
        Entity log = schema.addEntity("LogItem");
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

}