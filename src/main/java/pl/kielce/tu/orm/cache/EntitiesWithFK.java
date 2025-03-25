package pl.kielce.tu.orm.cache;

import java.util.HashSet;
import java.util.Set;

public class EntitiesWithFK {
    private static final EntitiesWithFK INSTANCE = new EntitiesWithFK();
    private final Set<String> entities;

    private EntitiesWithFK() {
        this.entities = new HashSet<>();
    }

    public static EntitiesWithFK getInstance() {
        return INSTANCE;
    }

    public Set<String> getEntities() {
        return Set.copyOf(this.entities);
    }

    public void addEntity(String entity) {
        this.entities.add(entity);
    }
}
