package pl.kielce.tu.orm.dialects;

import pl.kielce.tu.orm.exceptions.UnknownTypeException;

import java.time.Instant;
import java.util.UUID;

public class PostgreSQLDialect implements SQLDialect {
    @Override
    public String createTable() {
        return "CREATE TABLE IF NOT EXISTS";
    }

    @Override
    public String dataType(Class<?> type) throws UnknownTypeException {
        if (String.class.equals(type)) {
            return "varchar(255)";
        } else if (Integer.class.equals(type)) {
            return "integer";
        } else if (Long.class.equals(type)) {
            return "bigint";
        } else if (Boolean.class.equals(type)) {
            return "boolean";
        } else if (Float.class.equals(type) || Double.class.equals(type)) {
            return "real";
        } else if (Instant.class.equals(type)) {
            return "timestamp";
        } else if (UUID.class.equals(type)) {
            return "uuid";
        } else {
            throw new UnknownTypeException("Unknown type: " + type);
        }
    }

    @Override
    public String uniqueConstraint() {
        return "UNIQUE";
    }

    @Override
    public String notNull() {
        return "NOT NULL";
    }

    @Override
    public String identity() {
        return "bigserial PRIMARY KEY NOT NULL";
    }

    @Override
    public String identityType() {
        return "bigserial";
    }

    @Override
    public String addConstraint(String tableName, String constraintName, String foreignKeyName,
                                String referencedTableName, String referencedColumnName) {
        return "ALTER TABLE " + tableName +
                " ADD CONSTRAINT " +
                constraintName +
                " FOREIGN KEY (" +
                foreignKeyName +
                ") REFERENCES " +
                referencedTableName +
                "(" +
                referencedColumnName +
                ");";
    }
}
