package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.dialects.SQLDialect;
import pl.kielce.tu.orm.sql.SQLNamesHelper;

public class DatabaseTableCreator {
    private static final Logger log = LoggerFactory.getLogger(DatabaseTableCreator.class);
    private final String className;
    private final SQLDialect dialect;
    private final SQLNamesHelper sqlNamesHelper;

    public DatabaseTableCreator(String className) {
        this(className, new PostgreSQLDialect());
    }

    public DatabaseTableCreator(String className, SQLDialect dialect) {
        this.className = className;
        this.dialect = dialect;
        this.sqlNamesHelper = new SQLNamesHelper(className);
    }

    public String getSQLStatement() throws ClassNotFoundException {
        Class<?> entityClass = Class.forName(className);
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            log.error("No @Entity annotation found for class: {}", className);
            throw new IllegalStateException("No @Entity annotation found for class: " + className);
        }

        String tableName = sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());

        DatabaseColumnCreator columnCreator = new DatabaseColumnCreator(className, dialect);

        StringBuilder query = new StringBuilder(dialect.createTable());
        query.append(" ");
        query.append(tableName);
        query.append(" (\n");
        query.append(columnCreator.getSQLStatement());
        query.replace(query.length() - 2, query.length() - 1, "");
        query.append(");");

        return query.toString();
    }
}
