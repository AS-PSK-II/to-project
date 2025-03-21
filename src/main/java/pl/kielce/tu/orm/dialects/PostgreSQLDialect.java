package pl.kielce.tu.orm.dialects;

public class PostgreSQLDialect implements SQLDialect {
    @Override
    public String createTable() {
        return "CREATE TABLE IF NOT EXISTS";
    }

    @Override
    public String dataType(Class<?> type) {
        if (String.class.equals(type)) {
            return "varchar";
        } else if (Integer.class.equals(type)) {
            return "int";
        } else if (Long.class.equals(type)) {
            return "bigint";
        } else {
            return "";
        }
    }
}
