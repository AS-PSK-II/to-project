package pl.kielce.tu.orm.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kielce.tu.orm.annotations.Entity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EntitiesClassLoader {
    private static final Logger log = LoggerFactory.getLogger(EntitiesClassLoader.class.getName());

    public Set<Class<?>> findEntities(String packageName) {
        Set<Class<?>> classes = findClasses(packageName);

        return classes
                .stream()
                .filter(entityClass -> entityClass.getAnnotation(Entity.class) != null)
                .collect(Collectors.toSet());
    }

    public Set<Class<?>> findClasses(String packageName) {
        return getClasses(packageName)
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Set<Optional<Class<?>>> getClasses(String packageName) {
        Set<Optional<Class<?>>> result = new HashSet<>();

        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        if (stream == null) {
            throw new IllegalArgumentException("Package not found: " + packageName);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        List<String> lines = reader.lines().toList();
        for (String line : lines) {
            if (!line.contains(".")) {
                String newPackageName = !packageName.isEmpty() ? packageName + "." + line : line;
                result.addAll(getClasses(newPackageName));
            } else if (line.endsWith(".class")) {
                result.add(getClass(line, packageName));
            }
        }

        return result;
    }

    private Optional<Class<?>> getClass(String className, String packageName) {
        try {
            return Optional.of(
                    Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')))
            );
        } catch (ClassNotFoundException e) {
            log.warn("Class not found: {}", className);
        }

        return Optional.empty();
    }
}
