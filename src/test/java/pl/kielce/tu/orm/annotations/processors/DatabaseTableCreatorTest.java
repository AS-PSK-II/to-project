package pl.kielce.tu.orm.annotations.processors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.cache.EntitiesWithFK;
import pl.kielce.tu.orm.cache.ManyToManyTables;
import pl.kielce.tu.orm.classloader.EntitiesClassLoader;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTableCreatorTest {

    @AfterEach
    void cleanup() {
        EntitiesWithFK.getInstance().clear();
        ManyToManyTables.getInstance().clear();
    }

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
\tparent bigint UNIQUE
);""","""
CREATE TABLE IF NOT EXISTS PARENT (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL,
\tchild bigint UNIQUE
);"""};

        for (int i = 0; i < entities.size(); i++) {
            DatabaseTableCreator creator = new DatabaseTableCreator(entities.get(i).getName());
            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals(expectedValues[i], sqlStatement);
        }
    }

    @Test
    void shouldAddTablesWithOneToOneForeignKeysToCache() {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetoone";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        EntitiesWithFK entitiesWithFK = EntitiesWithFK.getInstance();

        assertEquals(2, entitiesWithFK.getEntities().size());
    }

    @Test
    void shouldGenerateAddConstraintForForeignKeyTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetoone";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        EntitiesWithFK entitiesWithFKCache = EntitiesWithFK.getInstance();
        List<String> entitiesWithFk = entitiesWithFKCache.getEntities()
                                   .stream()
                                   .sorted(Comparator.comparing(String::toString))
                                   .toList();
        String[] expectedValues = new String[] {"""
ALTER TABLE CHILD ADD CONSTRAINT fk_child_parent FOREIGN KEY (parent) REFERENCES PARENT(id);""",
                """
ALTER TABLE PARENT ADD CONSTRAINT fk_parent_child FOREIGN KEY (child) REFERENCES CHILD(id);"""
        };

        for (int i = 0; i < entitiesWithFk.size(); i++) {
            DatabaseForeignKeyCreator creator = new DatabaseForeignKeyCreator(entitiesWithFk.get(i));

            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals(expectedValues[i], sqlStatement);
        }
    }

    @Test
    void shouldCreateSQLStatementForEntityWithManyToOneForeignKeyTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        List<Class<?>> entities = entitiesClassLoader.findEntities(packageName)
                .stream()
                .sorted(Comparator.comparing(Class::getName))
                .toList();

        String[] expectedValues = new String[] {"""
CREATE TABLE IF NOT EXISTS CHILD (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL,
\tparent bigint NOT NULL
);""","""
CREATE TABLE IF NOT EXISTS PARENT (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL
);"""};

        for (int i = 0; i < entities.size(); i++) {
            DatabaseTableCreator creator = new DatabaseTableCreator(entities.get(i).getName());
            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals(expectedValues[i], sqlStatement);
        }
    }

    @Test
    void shouldAddTablesWithOneToManyAndManyToOneForeignKeysToCache() {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        EntitiesWithFK entitiesWithFK = EntitiesWithFK.getInstance();

        assertEquals(1, entitiesWithFK.getEntities().size());
    }

    @Test
    void shouldGenerateAddConstraintForOneToManyAndManyToOneForeignKeyTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.onetomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        EntitiesWithFK entitiesWithFKCache = EntitiesWithFK.getInstance();
        List<String> entitiesWithFk = entitiesWithFKCache.getEntities()
                .stream()
                .sorted(Comparator.comparing(String::toString))
                .toList();

        DatabaseForeignKeyCreator creator = new DatabaseForeignKeyCreator(entitiesWithFk.getFirst());

        String sqlStatement = creator.getSQLStatement();

        assertNotNull(sqlStatement);
        assertEquals("ALTER TABLE CHILD ADD CONSTRAINT fk_child_parent FOREIGN KEY (parent) REFERENCES PARENT(id);", sqlStatement);
    }

    @Test
    void shouldAddTablesWithManyToManyForeignKeysToCache() {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.manytomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        ManyToManyTables manyToManyTables = ManyToManyTables.getInstance();

        assertEquals(1, manyToManyTables.getColumnDefinitions().size());
    }

    @Test
    void shouldAddTablesWithManyToManyForeignKeysToCacheOnlyOnce() {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.manytomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        Set<Class<?>> entities = entitiesClassLoader.findEntities(packageName);
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        entities.forEach(entity -> {
            DatabaseTableCreator creator = new DatabaseTableCreator(entity.getName());
            try {
                creator.getSQLStatement();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        ManyToManyTables manyToManyTables = ManyToManyTables.getInstance();

        assertEquals(1, manyToManyTables.getColumnDefinitions().size());
    }

    @Test
    void shouldGenerateAdditionalTableAndAddConstraintForManyToManyForeignKeyTableName() throws Exception {
        String packageName = "pl.kielce.tu.orm.annotations.processors.db.manytomany";
        EntitiesClassLoader entitiesClassLoader = new EntitiesClassLoader();
        List<Class<?>> entities = entitiesClassLoader.findEntities(packageName)
                .stream()
                .sorted(Comparator.comparing(Class::getName))
                .toList();

        String[] expectedValues = new String[] {"""
CREATE TABLE IF NOT EXISTS FIRST_ENTITY (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL
);""","""
CREATE TABLE IF NOT EXISTS SECOND_ENTITY (
\tid bigserial PRIMARY KEY NOT NULL,
\tname varchar(255) NOT NULL
);"""};

        for (int i = 0; i < entities.size(); i++) {
            DatabaseTableCreator creator = new DatabaseTableCreator(entities.get(i).getName());
            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals(expectedValues[i], sqlStatement);
        }

        ManyToManyTables manyToManyTables = ManyToManyTables.getInstance();
        manyToManyTables.getColumnDefinitions().forEach(columnDefinition -> {
            ManyToManyCreator creator = new ManyToManyCreator(columnDefinition);

            String sqlStatement = creator.getSQLStatement();

            assertNotNull(sqlStatement);
            assertEquals("""
CREATE TABLE IF NOT EXISTS FIRST_ENTITY_SECOND_ENTITY (
first_entity_id bigserial NOT NULL,
second_entity_id bigserial NOT NULL
);
ALTER TABLE FIRST_ENTITY_SECOND_ENTITY ADD CONSTRAINT fk_first_entity_second_entity_first_entity FOREIGN KEY (first_entity_id) REFERENCES FIRST_ENTITY(id);
ALTER TABLE FIRST_ENTITY_SECOND_ENTITY ADD CONSTRAINT fk_first_entity_second_entity_second_entity FOREIGN KEY (second_entity_id) REFERENCES SECOND_ENTITY(id);""", sqlStatement);
        });
    }
}
