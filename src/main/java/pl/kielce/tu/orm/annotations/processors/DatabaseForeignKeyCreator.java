package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Column;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.OneToOne;
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

        Class<?> childEntityClass = getChildEntityClass(field).orElseThrow();
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

    private Optional<Class<?>> getChildEntityClass(Field field) {
        OneToOne onetoOneAnnotation = field.getAnnotation(OneToOne.class);

        if (onetoOneAnnotation != null) {
            return Optional.of(onetoOneAnnotation.child());
        }

        return Optional.empty();
    }
}
