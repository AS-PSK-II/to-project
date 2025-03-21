package pl.kielce.tu.orm.dialects;

public interface SQLDialect {
    String createTable();
    String dataType(Class<?> type);
}
