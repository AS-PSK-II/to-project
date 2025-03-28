package pl.kielce.tu.orm.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.config.ORMConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnector {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnector.class);
    private static final DatabaseConnector instance = new DatabaseConnector();
    private final String connectionString;
    private final String username;
    private final String password;
    private final String dbDriver;
    private Connection connection;

    private DatabaseConnector() {
        ORMConfiguration config = ORMConfiguration.getInstance();

        this.connectionString = config.getProperty("connectionString");
        this.username = config.getProperty("username");
        this.password = config.getProperty("password");
        this.dbDriver = config.getProperty("dbDriver");
    }

    public static DatabaseConnector getInstance() {
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            try {
                Constructor<?> connectionConstructor = Class.forName(dbDriver).getConstructor();
                connectionConstructor.newInstance();
                connection = DriverManager.getConnection(connectionString, username, password);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                     InstantiationException | IllegalAccessException e) {
                log.error("Could not find JDBC driver. Make sure it is in the classpath", e);
                System.exit(1);
            } catch (SQLException e) {
                log.error("Could not connect to database. Make sure the database server is available", e);
                System.exit(1);
            }
        }

        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Could not close database connection", e);
            }
        }
    }
}
