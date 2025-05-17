package pl.kielce.tu.orm.annotations.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.definitions.ManyToManyColumnDefinition;
import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.dialects.SQLDialect;
import pl.kielce.tu.orm.sql.SQLNamesHelper;

import java.util.*;
import java.util.stream.Stream;

public class ManyToManyCreator {
    private static final Logger log = LoggerFactory.getLogger(ManyToManyCreator.class);
    private final ManyToManyColumnDefinition manyToManyColumn;
    private final SQLDialect dialect;

    public ManyToManyCreator(ManyToManyColumnDefinition manyToManyColumn) {
        this(manyToManyColumn, new PostgreSQLDialect());
    }

    public ManyToManyCreator(ManyToManyColumnDefinition manyToManyColumn, SQLDialect dialect) {
        this.manyToManyColumn = manyToManyColumn;
        this.dialect = dialect;
    }

    public String getSQLStatement() {
        List<Class<?>> sortedEntities = sortEntitiesByName(manyToManyColumn.firstTable(), manyToManyColumn.secondTable());
        Class<?> firstEntityClass = sortedEntities.get(0);
        Class<?> secondEntityClass = sortedEntities.get(1);

        return new StringBuilder(generateSQLForNewTable(firstEntityClass, secondEntityClass))
                .append("\n")
                .append(generateForeignKeyDefinitions(firstEntityClass, secondEntityClass))
                .toString();
    }

    private String generateSQLForNewTable(Class<?> firstEntityClass, Class<?> secondEntityClass) {
        String firstTableName = getTableName(firstEntityClass);
        String secondTableName = getTableName(secondEntityClass);
        String tableName = firstTableName + "_" + secondTableName;

        StringBuilder query = new StringBuilder(dialect.createTable())
                .append(" ")
                .append(tableName)
                .append(" (\n")
                .append(getColumnDefinition(firstTableName))
                .append(",\n")
                .append(getColumnDefinition(secondTableName))
                .append("\n);");

        return query.toString();
    }

    private String generateForeignKeyDefinitions(Class<?> firstEntityClass, Class<?> secondEntityClass) {
        String firstTableName = getTableName(firstEntityClass);
        String secondTableName = getTableName(secondEntityClass);
        String tableName = firstTableName + "_" + secondTableName;
        String firstConstraintName = "fk_" + tableName.toLowerCase() + "_" + firstTableName.toLowerCase();
        String secondConstraintName = "fk_" + tableName.toLowerCase() + "_" + secondTableName.toLowerCase();

        return new StringBuilder(dialect.addConstraint(tableName, firstConstraintName, firstTableName.toLowerCase() + "_id", firstTableName, "id"))
                .append("\n")
                .append(dialect.addConstraint(tableName, secondConstraintName, secondTableName.toLowerCase() + "_id", secondTableName, "id"))
                .toString();
    }

    private String getTableName(Class<?> entityClass) {
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            log.error("No @Entity annotation found for class: {}", entityClass.getName());
            throw new IllegalStateException("No @Entity annotation found for class: " + entityClass.getName());
        }

        SQLNamesHelper sqlNamesHelper = new SQLNamesHelper(entityClass.getName());

        return sqlNamesHelper.getTableName(entityClass, entityAnnotation.name());
    }

    private List<Class<?>> sortEntitiesByName(Class<?> firstEntity, Class<?> secondEntity) {
        return Stream.of(firstEntity, secondEntity)
                .sorted(Comparator.comparing(Class::getSimpleName))
                .toList();
    }

    private String getColumnDefinition(String tableName) {
        return new StringBuilder(tableName.toLowerCase())
                .append("_id")
                .append(" ")
                .append(dialect.identityType())
                .append(" ")
                .append(dialect.notNull())
                .toString();
    }
}
