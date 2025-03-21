package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.TOEntity;
import pl.kielce.tu.orm.dialects.SQLDialect;

import java.lang.reflect.Field;

public class DatabaseTableCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseTableCreator.class);
    private final String className;
    private final SQLDialect dialect;

    public DatabaseTableCreator(String className, SQLDialect dialect) {
        this.className = className;
        this.dialect = dialect;
    }

    public String getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        TOEntity entityAnnotation = entityClass.getAnnotation(TOEntity.class);
        if (entityAnnotation == null) {
            log.error("No @TOEntity annotation found for class: {}", className);
            throw new IllegalStateException("No @TOEntity annotation found for class: " + className);
        }

        Field[] fields = entityClass.getDeclaredFields();

        StringBuilder query = new StringBuilder(dialect.createTable());
        query.append(" CREATE TABLE ");
        query.append(getTableName(entityClass, entityAnnotation.name()));
        query.append(" (");
        for (Field field : fields) {
            query.append(getColumnDeclaration(field));
        }
        query.append(");");

        return query.toString();
    }

    private String getTableName(Class<?> entityClass, String tableName) {
        String result = tableName;
        if (tableName == null || tableName.isBlank()) {
            log.info("Table name is null or empty for class: {}. Use default table name", className);
            result = toUnderscoreName(entityClass.getSimpleName());
        }

        return result.toUpperCase();
    }

    private String getColumnDeclaration(Field field) {
        StringBuilder column = new StringBuilder();
        column.append(toUnderscoreName(field.getName()));
        column.append(" ");
        column.append(field.getType().getSimpleName());
        column.append(" ");
        column.append("not null, ");
        return column.toString();
    }

    private String toUnderscoreName(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                result.append("_");
            }
            result.append(Character.toLowerCase(ch));
        }

        return result.toString();
    }
}
