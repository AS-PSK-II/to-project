package pl.kielce.tu.orm.annotations.processors;

import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTableCreatorTest {
    @Test
    void shouldReturnCorrectCreateTableSQLStatement() throws Exception {
        String packageName = "pl.kielce.tu.orm.db";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Optional<Class<?>>> classes = entitiesClassLoader.findEntities(packageName);

        for (Optional<Class<?>> entity : classes) {
            if (entity.isPresent()) {
                DatabaseTableCreator creator = new DatabaseTableCreator(entity.get().getName());
                Optional<String> sqlStatement = creator.getSQLStatement();
                assertTrue(sqlStatement.isPresent());
                assertNotNull(sqlStatement.get());
            }
        }

    }
}
