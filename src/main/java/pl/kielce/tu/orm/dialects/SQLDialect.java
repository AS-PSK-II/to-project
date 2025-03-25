package pl.kielce.tu.orm.dialects;

import pl.kielce.tu.orm.exceptions.UnknownTypeException;

public interface SQLDialect {
    String createTable();

    String dataType(Class<?> type) throws UnknownTypeException;

    String uniqueConstraint();

    String notNull();

    String identity();

    String addConstraint(String tableName, String constraintName, String foreignKeyName, String referencedTableName,
                         String referencedColumnName);
}
