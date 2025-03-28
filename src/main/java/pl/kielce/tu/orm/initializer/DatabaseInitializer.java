package pl.kielce.tu.orm.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.processors.DatabaseTableCreator;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;
import pl.kielce.tu.orm.config.ORMConfiguration;
import pl.kielce.tu.orm.connector.DatabaseConnector;

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
                    log.error("Cannot execure SQL statement for class {}", entity.getName(), e);
                }
            });

            try {
                dbConnection.close();
            } catch (Exception e) {
                log.error("Cannot close database connection", e);
            }

            isInitialized = true;
            log.info("Database is successfully initialized.");
        } else {
            log.warn("Database is already initialized");
        }
    }

    private static void setConfigProperties(String connectionString, String username, String password, String dbDriver) {
        ORMConfiguration config = ORMConfiguration.getInstance();
        config.addProperty("connectionString", connectionString);
        config.addProperty("username", username);
        config.addProperty("password", password);
        config.addProperty("dbDriver", dbDriver);
    }
}
