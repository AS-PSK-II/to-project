package pl.kielce.tu.orm.definitions;

public record ManyToManyColumnDefinition(Class<?> firstTable, Class<?> secondTable) {
}
