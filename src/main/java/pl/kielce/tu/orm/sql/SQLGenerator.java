package pl.kielce.tu.orm.sql;

import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToOne;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SQLGenerator {

    public static String generateInsertSQL(Object entity, String tableName, List<Field> fields) {
        StringJoiner columns = new StringJoiner(", ", "(", ")");
        StringJoiner values = new StringJoiner(", ", "(", ")");

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entity.getClass().getName());
                    String columnName = sqlNamesHelper.getColumnName(field, "");

                    if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                        if (SQLAnnotationsHelper.hasOneToOneAnnotation(field) || field.isAnnotationPresent(ManyToOne.class)) {
                            Field idField = getIdField(value.getClass());
                            idField.setAccessible(true);
                            Object idValue = idField.get(value);

                            if (idValue != null) {
                                columns.add(columnName);
                                values.add("?");
                            }
                        }
                    } else if (!SQLAnnotationsHelper.hasOneToManyAnnotation(field)) {
                        columns.add(columnName);
                        values.add("?");
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field: " + field.getName(), e);
            }
        }

        return "INSERT INTO " + tableName + " " + columns + " VALUES " + values;
    }

    public static String generateUpdateSQL(String tableName, List<Field> fields, Field idField) {
        StringJoiner setClause = new StringJoiner(", ");

        for (Field field : fields) {
            if (!field.equals(idField)) {
                SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(field.getDeclaringClass().getName());
                String columnName = sqlNamesHelper.getColumnName(field, "");

                if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                    if (SQLAnnotationsHelper.hasOneToOneAnnotation(field) || field.isAnnotationPresent(ManyToOne.class)) {
                        setClause.add(columnName + " = ?");
                    }
                } else if (!SQLAnnotationsHelper.hasOneToManyAnnotation(field)) {
                    setClause.add(columnName + " = ?");
                }
            }
        }

        SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(idField.getDeclaringClass().getName());
        String idColumnName = sqlNamesHelper.getColumnName(idField, "");
        return "UPDATE " + tableName + " SET " + setClause + " WHERE " + idColumnName + " = ?";
    }

    public static String generateSelectSQL(String tableName, List<Field> fields) {
        return "SELECT * FROM " + tableName;
    }

    public static String generateSelectByIdSQL(String tableName, List<Field> fields, Field idField) {
        String selectSQL = generateSelectSQL(tableName, fields);
        SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(idField.getDeclaringClass().getName());
        String idColumnName = sqlNamesHelper.getColumnName(idField, "");
        return selectSQL + " WHERE " + idColumnName + " = ?";
    }

    public static String generateDeleteSQL(String tableName, Field idField) {
        SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(idField.getDeclaringClass().getName());
        String idColumnName = sqlNamesHelper.getColumnName(idField, "");
        return "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
    }

    public static String generateDeleteAllSQL(String tableName) {
        return "DELETE FROM " + tableName;
    }

    public static String generateCountSQL(String tableName) {
        return "SELECT COUNT(*) FROM " + tableName;
    }

    public static Field getIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No @Id field found in class: " + entityClass.getName());
    }

    public static List<Field> getFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            fields.add(field);
        }
        return fields;
    }

    public static List<Field> getNonRelationshipFields(Class<?> entityClass) {
        return getFields(entityClass).stream()
                .filter(field -> !SQLAnnotationsHelper.hasForeignTableAnnotation(field))
                .collect(Collectors.toList());
    }
}
