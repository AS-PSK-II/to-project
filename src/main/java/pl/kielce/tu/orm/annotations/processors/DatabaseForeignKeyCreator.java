package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.*;
import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.dialects.SQLDialect;

import java.lang.reflect.Field;
import java.util.Optional;

public class DatabaseForeignKeyCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseForeignKeyCreator.class);
    private final String className;
    private final SQLDialect dialect;
    private final SQLNamesHelper sqlNamesHelper;

    public DatabaseForeignKeyCreator(String className) {
        this(className, new PostgreSQLDialect());
    }

    public DatabaseForeignKeyCreator(String className, SQLDialect dialect) {
        this.className = className;
        this.dialect = dialect;
        this.sqlNamesHelper = new SQLNamesHelper(className);
    }

    public String getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);

        if (entityAnnotation == null) {
            log.warn("Entity annotation not found in class {}", className);
            throw new IllegalStateException("No @Entity annotation found for class: " + className);
        }

        StringBuilder statement = new StringBuilder();
        String tableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());
        Field[] fields = entityClass.getDeclaredFields();

        for (Field field : fields) {
            if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                statement.append(getForeignKeyConstraint(field, tableName))
                         .append("\n");
            }
        }

        return statement.substring(0, statement.length() - 1);
    }

    private String getForeignKeyConstraint(Field field, String tableName) {
        Column columnAnnotation = field.getAnnotation(Column.class);

        if (columnAnnotation == null) {
            log.info("No @Column annotation found for field: {}. Use default values", field.getName());
        }

        Class<?> childEntityClass = getChildEntityClass(field);
        Entity childEntityAnnotation = childEntityClass.getAnnotation(Entity.class);

        if (childEntityAnnotation == null) {
            log.error("No @Entity annotation found for class: {}", childEntityClass.getName());
            throw new IllegalStateException("No @Entity annotation found for class: " + childEntityClass.getName());
        }

        String columnName = sqlNamesHelper.getColumnName(field, columnAnnotation != null ? columnAnnotation.name() : "");
        String foreignTableName = sqlNamesHelper.getTableName(childEntityClass, childEntityAnnotation.name());
        String constraintName = "fk_" + tableName.toLowerCase() + "_" + columnName.toLowerCase();

        return dialect.addConstraint(tableName, constraintName, columnName, foreignTableName, "id");
    }

    private Class<?> getChildEntityClass(Field field) {
        Optional<Class<?>> childEntityClass = Optional.empty();
        OneToOne onetoOneAnnotation = field.getAnnotation(OneToOne.class);
        ManyToOne oneToManyAnnotation = field.getAnnotation(ManyToOne.class);

        if (onetoOneAnnotation != null) {
            childEntityClass = Optional.of(onetoOneAnnotation.entity());
        } else if (oneToManyAnnotation != null) {
            childEntityClass = Optional.of(oneToManyAnnotation.entity());
        }

        return childEntityClass.orElseThrow(
                () -> new IllegalStateException("No foreign key entity found for field: " + field.getName())
        );
    }
}
