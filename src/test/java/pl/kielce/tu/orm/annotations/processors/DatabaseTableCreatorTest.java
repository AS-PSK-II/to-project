package pl.kielce.tu.orm.annotations.processors;

import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTableCreatorTest {
    @Test
    void shouldCreateSQLStatementForEntityWithDefaultTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.defaultname";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);

        for (Class<?> entity : entities) {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            String sqlStatement = creator.getSQLStatement();
            assertNotNull(sqlStatement);
            assertEquals("CREATE TABLE IF NOT EXISTS TEST_DEFAULT_NAME (id bigint not null, name varchar(255) not null, age integer not null );", sqlStatement);
        }

    }
}
