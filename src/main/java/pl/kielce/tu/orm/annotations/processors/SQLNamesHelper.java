package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class SQLNamesHelper {
    private static final Logger log = LoggerFactory.getLogger(SQLNamesHelper.class);
    private final String className;

    public SQLNamesHelper(String className) {
        this.className = className;
    }

    public String getTableName(Class<?> entityClass, String tableName) {
        String result = tableName;
        if (tableName == null || tableName.isBlank()) {
            log.info("Table name is null or empty for class: {}. Use default table name", className);
            result = toUnderscoreName(entityClass.getSimpleName());
        }

        return result.toUpperCase();
    }

    public String toUnderscoreName(String name) {
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

    public String getColumnName(Field field, String columnName) {
        String result = columnName;
        if (columnName == null || columnName.isBlank()) {
            log.info("Column name is null or empty for class: {}. Use default table name", className);
            result = toUnderscoreName(field.getName());
        }

        return result;
    }
}
