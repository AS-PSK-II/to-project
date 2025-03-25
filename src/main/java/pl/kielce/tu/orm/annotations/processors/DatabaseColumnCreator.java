package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Column;
import pl.kielce.tu.orm.cache.EntitiesWithFK;
import pl.kielce.tu.orm.dialects.SQLDialect;
import pl.kielce.tu.orm.exceptions.UnknownTypeException;

import java.lang.reflect.Field;

public class DatabaseColumnCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseColumnCreator.class);
    private final String className;
    private final SQLDialect dialect;
    private final SQLNamesHelper sqlNamesHelper;
    private final EntitiesWithFK entitiesWithFK;

    public DatabaseColumnCreator(String className, SQLDialect dialect) {
        this.className = className;
        this.dialect = dialect;
        this.sqlNamesHelper = new SQLNamesHelper(className);
        this.entitiesWithFK = EntitiesWithFK.getInstance();
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
            log.info("No @Column annotation found for field: {}. Use default values", field.getName());
        }

        String columnName = sqlNamesHelper.getColumnName(field, columnAnnotation != null ? columnAnnotation.name() :
                "");

        StringBuilder column = new StringBuilder();
        column.append(columnName);
        try {
            if (SQLAnnotationsHelper.hasIdAnnotation(field)) {
                column.append(" ")
                      .append(dialect.identity());
            } else if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                entitiesWithFK.addEntity(className);
                column.append(" ")
                      .append(dialect.dataType(Long.class))
                      .append(" ")
                      .append(dialect.notNull());
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
}
