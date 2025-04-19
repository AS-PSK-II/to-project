package pl.kielce.tu.orm.cache;

import pl.kielce.tu.orm.definitions.ManyToManyColumnDefinition;

import java.util.HashSet;
import java.util.Set;

public class ManyToManyTables {
    private static final ManyToManyTables INSTANCE = new ManyToManyTables();
    private final Set<ManyToManyColumnDefinition> columnDefinitions;

    private ManyToManyTables() {
        columnDefinitions = new HashSet<>();
    }

    public static ManyToManyTables getInstance() {
        return INSTANCE;
    }

    public Set<ManyToManyColumnDefinition> getColumnDefinitions() {
        return Set.copyOf(columnDefinitions);
    }

    public void addColumnDefinition(Class<?> firstTable, Class<?> secondTable) {
        addColumnDefinition(new ManyToManyColumnDefinition(firstTable, secondTable));
    }

    public void addColumnDefinition(ManyToManyColumnDefinition columnDefinition) {
        if (!containsColumnDefinition(columnDefinition.firstTable(), columnDefinition.secondTable())) {
            columnDefinitions.add(columnDefinition);
        }
    }

    public void clear() {
        columnDefinitions.clear();
    }

    public boolean containsColumnDefinition(Class<?> firstTable, Class<?> secondTable) {
        return columnDefinitions.stream().anyMatch((columnDefinition) ->
                (columnDefinition.firstTable().getName().equals(firstTable.getName())
                        || columnDefinition.secondTable().getName().equals(firstTable.getName()))
                        && (columnDefinition.firstTable().getName().equals(secondTable.getName())
                        || columnDefinition.secondTable().getName().equals(secondTable.getName())));
    }
}
