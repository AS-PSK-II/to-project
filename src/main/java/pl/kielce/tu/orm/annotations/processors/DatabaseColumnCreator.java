package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Column;
import pl.kielce.tu.orm.annotations.ManyToMany;
import pl.kielce.tu.orm.cache.EntitiesWithFK;
import pl.kielce.tu.orm.cache.ManyToManyTables;
import pl.kielce.tu.orm.dialects.SQLDialect;
import pl.kielce.tu.orm.exceptions.UnknownTypeException;
import pl.kielce.tu.orm.sql.SQLAnnotationsHelper;
import pl.kielce.tu.orm.sql.SQLNamesHelper;

import java.lang.reflect.Field;

public class DatabaseColumnCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseColumnCreator.class);
    private final String className;
    private final SQLDialect dialect;
    private final SQLNamesHelper sqlNamesHelper;
    private final EntitiesWithFK entitiesWithFK;
    private final ManyToManyTables manyToManyTables;

    public DatabaseColumnCreator(String className, SQLDialect dialect) {
        this.className = className;
        this.dialect = dialect;
        this.sqlNamesHelper = new SQLNamesHelper(className);
        this.entitiesWithFK = EntitiesWithFK.getInstance();
        this.manyToManyTables = ManyToManyTables.getInstance();
    }

    public String getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        Field[] fields = entityClass.getDeclaredFields();

        StringBuilder query = new StringBuilder();

        for (Field field : fields) {
            query.append(getColumnDeclaration(field));
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
        try {
            if (SQLAnnotationsHelper.hasIdAnnotation(field)) {
                column.append("\t")
                        .append(columnName);
                addIdColumn(column);
            } else if (SQLAnnotationsHelper.hasForeignTableAnnotation(field)) {
                addColumnWithForeignKey(field, column, columnName);
            } else {
                column.append("\t")
                        .append(columnName);
                addColumnSQLDefinition(field, column, columnAnnotation);
            }
        } catch (UnknownTypeException e) {
            log.info("Unknown type for field: {}. Start custom data type processing", field.getName());
            return "";
        }

        return column.toString();
    }

    private void addIdColumn(StringBuilder column) {
        column.append(" ")
                .append(dialect.identity())
                .append(",\n");
    }

    private void addColumnWithForeignKey(Field field, StringBuilder column, String columnName) throws UnknownTypeException {
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);
        if (manyToManyAnnotation != null) {
            addManyToManyTableToCache(manyToManyAnnotation);
            return;
        }

        entitiesWithFK.addEntity(className);
        column.append("\t")
                .append(columnName)
                .append(" ")
                .append(dialect.dataType(Long.class))
                .append(SQLAnnotationsHelper.hasOneToOneAnnotation(field) ? " " + dialect.uniqueConstraint() : "")
                .append(SQLAnnotationsHelper.hasOneToOneAnnotation(field) ? "" : " " + dialect.notNull())
                .append(",\n");
    }

    private void addManyToManyTableToCache(ManyToMany manyToManyAnnotation) {
        try {
            manyToManyTables.addColumnDefinition(Class.forName(className), manyToManyAnnotation.entity());
        } catch (ClassNotFoundException e) {
            log.error("Cannot add manyToMany table to class {}", className, e);
        }
    }

    private void addColumnSQLDefinition(Field field, StringBuilder column, Column columnAnnotation) throws UnknownTypeException {
        column.append(" ")
                .append(dialect.dataType(field.getType()))
                .append(" ")
                .append(columnAnnotation != null && columnAnnotation.nullable() ? "" : dialect.notNull())
                .append(columnAnnotation != null && columnAnnotation.unique() ? dialect.uniqueConstraint() : "")
                .append(",\n");
    }
}
