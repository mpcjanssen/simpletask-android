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
        Schema schema = new Schema(1012, "nl.mpcjanssen.simpletask.dao.gen");
        addEntities(schema);
        new DaoGenerator().generateAll(schema, "app/src/main/java");
    }

    private static void addEntities(Schema schema) {
        backupSchema(schema);
    }

    private static void backupSchema(Schema schema) {
        Entity entry = schema.addEntity("TodoFile");
        entry.addStringProperty("contents").notNull().primaryKey();
        entry.addStringProperty("name").notNull();
        entry.addDateProperty("date").notNull();
    }
}
