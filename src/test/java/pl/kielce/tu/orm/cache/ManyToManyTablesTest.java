package pl.kielce.tu.orm.cache;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManyToManyTablesTest {
    private static ManyToManyTables manyToManyTables;

    @BeforeAll
    static void setUp() {
        manyToManyTables = ManyToManyTables.getInstance();
        manyToManyTables.addColumnDefinition(String.class, Integer.class);
    }

    @Test
    void shouldContainsDefinitionIfTablesAreEqualInTheSameOrder() {
        boolean containsColumnDefinition = manyToManyTables.containsColumnDefinition(String.class, Integer.class);

        assertTrue(containsColumnDefinition);
    }

    @Test
    void shouldContainsDefinitionIfTablesAreEqualInDifferentOrder() {
        boolean containsColumnDefinition = manyToManyTables.containsColumnDefinition(Integer.class, String.class);

        assertTrue(containsColumnDefinition);
    }

    @Test
    void shouldNotContainsDefinitionIfTablesAreNotEqual() {
        boolean containsColumnDefinition = manyToManyTables.containsColumnDefinition(String.class, Long.class);

        assertFalse(containsColumnDefinition);
    }
}
