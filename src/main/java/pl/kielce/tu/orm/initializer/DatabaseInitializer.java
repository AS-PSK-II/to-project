package pl.kielce.tu.orm.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.processors.DatabaseForeignKeyCreator;
import pl.kielce.tu.orm.annotations.processors.DatabaseTableCreator;
import pl.kielce.tu.orm.annotations.processors.ManyToManyCreator;
import pl.kielce.tu.orm.cache.EntitiesWithFK;
import pl.kielce.tu.orm.cache.ManyToManyTables;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;
import pl.kielce.tu.orm.config.ORMConfiguration;
import pl.kielce.tu.orm.connector.DatabaseConnector;
import pl.kielce.tu.orm.definitions.ManyToManyColumnDefinition;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;

public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static boolean isInitialized = false;

    private DatabaseInitializer() {}

    public static void initialize(String connectionString, String username, String password, String dbDriver) {
        if (!isInitialized) {
            log.info("Initializing Database...");

            setConfigProperties(connectionString, username, password, dbDriver);
            DatabaseConnector connector = DatabaseConnector.getInstance();
            Connection dbConnection = connector.getConnection();

            createTables(dbConnection);
            createForeignKeys(dbConnection);
            createManyToManyReferences(dbConnection);

            isInitialized = true;
            log.info("Database is successfully initialized.");
        } else {
            log.warn("Database is already initialized");
        }
    }

    private static void createTables(Connection dbConnection) {
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities("");

        entities.forEach(entity -> {
            DatabaseTableCreator tableCreator = new DatabaseTableCreator(entity.getName());
            try {
                String query = tableCreator.getSQLStatement();
                Statement statement = dbConnection.createStatement();
                statement.executeUpdate(query);

                statement.close();
            } catch (Exception e) {
                log.error("Cannot execute create table SQL statement for class {}", entity.getName(), e);
            }
        });
    }

    private static void createForeignKeys(Connection dbConnection) {
        EntitiesWithFK fkCache = EntitiesWithFK.getInstance();
        Set<String> entities = fkCache.getEntities();

        entities.forEach(entity -> {
            DatabaseForeignKeyCreator foreignKeyCreator = new DatabaseForeignKeyCreator(entity);
            try {
                String query = foreignKeyCreator.getSQLStatement();
                Statement statement = dbConnection.createStatement();
                statement.executeUpdate(query);

                statement.close();
            } catch (Exception e) {
                log.error("Cannot execute add foreign key SQL statement for class {}", entity, e);
            }
        });
    }

    private static void createManyToManyReferences(Connection dbConnection) {
        ManyToManyTables manyToManyTables = ManyToManyTables.getInstance();
        Set<ManyToManyColumnDefinition> tables = manyToManyTables.getColumnDefinitions();

        tables.forEach(table -> {
            ManyToManyCreator manyToManyCreator = new ManyToManyCreator(table);
            try {
                String query = manyToManyCreator.getSQLStatement();
                Statement statement = dbConnection.createStatement();
                statement.executeUpdate(query);

                statement.close();
            } catch (Exception e) {
                log.error("Cannot execute create table for many to many references for classes: {} and {}", table.firstTable(), table.secondTable());
            }
        });
    }

    private static void setConfigProperties(String connectionString, String username, String password, String dbDriver) {
        ORMConfiguration config = ORMConfiguration.getInstance();
        config.addProperty("connectionString", connectionString);
        config.addProperty("username", username);
        config.addProperty("password", password);
        config.addProperty("dbDriver", dbDriver);
    }
}
