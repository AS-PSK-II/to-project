package pl.kielce.tu.orm.config;

import java.util.HashMap;
import java.util.Map;

public class ORMConfiguration {
    private static final Map<String, String> CONFIG = new HashMap<>();
    private static final ORMConfiguration INSTANCE = new ORMConfiguration();

    public static ORMConfiguration getInstance() {
        return INSTANCE;
    }

    public String getProperty(String key) {
        return CONFIG.get(key);
    }

    public boolean hasProperty(String key) {
        return CONFIG.containsKey(key);
    }

    public void addProperty(String key, String value) {
        CONFIG.put(key, value);
    }
}
