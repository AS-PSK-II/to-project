package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.TOEntity;

import java.util.Optional;

public class DatabaseTableCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseTableCreator.class);
    private final String className;

    public DatabaseTableCreator(String className) {
        this.className = className;
    }

    public Optional<String> getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        TOEntity entityAnnotation = entityClass.getAnnotation(TOEntity.class);
        if (entityAnnotation == null) {
            log.error("No @TOEntity annotation found for class: {}", className);
            return Optional.empty();
        }

        String tableName = entityAnnotation.name();
        if (tableName == null || tableName.isBlank()) {
            log.info("Table name is null or empty for class: {}. Use default table name", className);
            tableName = entityClass.getSimpleName().toUpperCase();
        }

        return Optional.of(tableName);
    }
}
