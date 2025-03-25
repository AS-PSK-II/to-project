package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Column;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.OneToOne;
import pl.kielce.tu.orm.dialects.SQLDialect;
import pl.kielce.tu.orm.exceptions.UnknownTypeException;

import java.lang.reflect.Field;

public class DatabaseColumnCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseColumnCreator.class);
    private final String className;
    private final String tableName;
    private final SQLDialect dialect;
    private final SQLNamesHelper sqlNamesHelper;

    public DatabaseColumnCreator(String className, String tableName, SQLDialect dialect) {
        this.className = className;
        this.tableName = tableName;
        this.dialect = dialect;
        this.sqlNamesHelper = new SQLNamesHelper(className);
    }

    public String getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        Field[] fields = entityClass.getDeclaredFields();

        StringBuilder query = new StringBuilder();

        for (Field field : fields) {
            query.append("\t").append(getColumnDeclaration(field));
        }

        return query.toString();
    }

    private String getColumnDeclaration(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);

        if (columnAnnotation == null) {
            log.info("No @TOColumn annotation found for field: {}. Use default values", field.getName());
        }

        String columnName = sqlNamesHelper.getColumnName(field, columnAnnotation != null ? columnAnnotation.name() :
                "");

        StringBuilder column = new StringBuilder();
        column.append(columnName);
        try {
            if (hasIdAnnotation(field)) {
                column.append(" ")
                      .append(dialect.identity());

            } else if (hasForeignTableAnnotation(field)) {
                column.append(" ")
                      .append(getForeignTableSQLStatement(field, columnName));
            } else {
                column.append(" ")
                      .append(dialect.dataType(field.getType()))
                      .append(" ")
                      .append(columnAnnotation != null && columnAnnotation.nullable() ? "" : dialect.notNull())
                      .append(columnAnnotation != null && columnAnnotation.unique() ? dialect.uniqueConstraint() : "");
            }
        } catch (UnknownTypeException e) {
            log.info("Unknown type for field: {}. Start custom data type processing", field.getName());
            return "";
        }
        column.append(",\n");
        return column.toString();
    }

    private boolean hasIdAnnotation(Field field) {
        Id idAnnotation = field.getAnnotation(Id.class);

        return idAnnotation != null;
    }

    private boolean hasForeignTableAnnotation(Field field) {
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        return oneToOneAnnotation != null;
    }

    private String getForeignTableSQLStatement(Field field, String columnName) throws UnknownTypeException {
        StringBuilder query = new StringBuilder(dialect.dataType(Long.class))
                .append(" ")
                .append(dialect.notNull())
                .append(",\n");
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        if (oneToOneAnnotation != null) {
            Class<?> childClass = oneToOneAnnotation.child();
            Entity childEntityAnnotation = childClass.getAnnotation(Entity.class);

            if (childEntityAnnotation == null) {
                log.error("No @Entity annotation found for class: {}", className);
                throw new IllegalStateException("No @Entity annotation found for class: " + className);
            }

            String foreignTableName = sqlNamesHelper.getTableName(childClass, oneToOneAnnotation.name()).toLowerCase();

            query.append("\tCONSTRAINT fk_")
                 .append(tableName.toLowerCase())
                 .append("_")
                 .append(foreignTableName)
                 .append(" FOREIGN KEY (")
                 .append(columnName)
                 .append(") REFERENCES ")
                 .append(foreignTableName)
                 .append(" (")
                 .append(columnName)
                 .append(")");


        }

        return query.toString();
    }
}
