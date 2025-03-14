package pl.kielce.tu.orm.classloader;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntitiesClassLoaderTest {

    @Test
    void shouldReadAllClassesFromPackage() {
        String packageName = "pl.kielce.tu.orm.db";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();

        Set<Optional<Class<?>>> classes = entitiesClassLoader.findClasses(packageName);

        assertEquals(3, classes.size());
    }

    @Test
    void shouldReadAllClassesWithTOEntityAnnotation() {
        String packageName = "pl.kielce.tu.orm.db";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();

        Set<Optional<Class<?>>> classes = entitiesClassLoader.findEntities(packageName);

        assertEquals(2, classes.size());
    }
}
