package pl.kielce.tu.orm.annotations.processors;

import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTableCreatorTest {
    @Test
    void shouldCreateSQLStatementForEntityWithDefaultTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.defaultname";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        Class<?> entity = entities.iterator().next();
        DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());

        String sqlStatement = creator.getSQLStatement();

        assertNotNull(sqlStatement);
        assertEquals("""
CREATE TABLE IF NOT EXISTS TEST_DEFAULT_NAME (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) UNIQUE,
\tage integer NOT NULL
);""", sqlStatement);
    }

    @Test
    void shouldCreateSQLStatementForEntityWithForeignKeyTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetoone";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        List<Class<?>> entities = entitiesClassLoader.findEntities(packageName)
                                                     .stream()
                                                     .sorted(Comparator.comparing(Class::getName))
                                                     .toList();

        String[] expectedValues = new String[] {"""
CREATE TABLE IF NOT EXISTS CHILD (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL,
\tparent bigint NOT NULL,
\tCONSTRAINT fk_child_parent FOREIGN KEY (parent) REFERENCES parent (parent)
);""","""
CREATE TABLE IF NOT EXISTS PARENT (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL,
\tchild bigint NOT NULL,
\tCONSTRAINT fk_parent_child FOREIGN KEY (child) REFERENCES child (child)
);"""};

        for (int i = 0; i < entities.size(); i++) {
            DatabaseTableCreator creator = new DatabaseTableCreator(entities.get(i).getName());
            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals(expectedValues[i], sqlStatement);
        }


    }
}
