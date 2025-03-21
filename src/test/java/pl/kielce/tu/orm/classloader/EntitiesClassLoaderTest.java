package pl.kielce.tu.orm.classloader;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntitiesClassLoaderTest {

    @Test
    void shouldReadAllClassesFromPackage() {
        String packageName = "pl.kielce.tu.orm.classloader.db";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();

        Set<Class<?>> classes = entitiesClassLoader.findClasses(packageName);

        assertEquals(2, classes.size());
    }

    @Test
    void shouldReadAllClassesWithTOEntityAnnotation() {
        String packageName = "pl.kielce.tu.orm.classloader.db";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();

        Set<Class<?>> classes = entitiesClassLoader.findEntities(packageName);

        assertEquals(1, classes.size());
    }
}
