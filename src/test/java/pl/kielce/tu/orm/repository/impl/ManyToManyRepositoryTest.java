package pl.kielce.tu.orm.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToMany;
import pl.kielce.tu.orm.connector.DatabaseConnector;
import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.initializer.DatabaseInitializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManyToManyRepositoryTest {

    private CrudRepositoryImpl<FirstEntity, Long> firstRepository;
    private CrudRepositoryImpl<SecondEntity, Long> secondRepository;
    private DatabaseConnector databaseConnector;

    @BeforeEach
    void setUp() throws SQLException {
        // Initialize database with connection parameters
        DatabaseInitializer.initialize(
            "jdbc:postgresql://localhost:5432/test",
            "postgres",
            "postgres",
            "org.postgresql.Driver",
                new PostgreSQLDialect()
        );

        // Get database connection
        databaseConnector = DatabaseConnector.getInstance();
        Connection connection = databaseConnector.getConnection();

        // Drop tables if they exist
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS first_entity_second_entity")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS first_entity")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS second_entity")) {
            statement.executeUpdate();
        }

        // Create repositories
        firstRepository = new CrudRepositoryImpl<>(FirstEntity.class);
        secondRepository = new CrudRepositoryImpl<>(SecondEntity.class);
    }

    @Test
    @Disabled
    void testManyToManyRelationship() {
        // Create first entities
        FirstEntity first1 = new FirstEntity();
        first1.setId(1L);
        first1.setName("First 1");

        FirstEntity first2 = new FirstEntity();
        first2.setId(2L);
        first2.setName("First 2");

        // Create second entities
        SecondEntity second1 = new SecondEntity();
        second1.setId(1L);
        second1.setName("Second 1");

        SecondEntity second2 = new SecondEntity();
        second2.setId(2L);
        second2.setName("Second 2");

        // Set relationships
        List<SecondEntity> secondEntities1 = new ArrayList<>();
        secondEntities1.add(second1);
        secondEntities1.add(second2);
        first1.setSecondEntities(secondEntities1);

        List<SecondEntity> secondEntities2 = new ArrayList<>();
        secondEntities2.add(second1);
        first2.setSecondEntities(secondEntities2);

        List<FirstEntity> firstEntities1 = new ArrayList<>();
        firstEntities1.add(first1);
        firstEntities1.add(first2);
        second1.setFirstEntities(firstEntities1);

        List<FirstEntity> firstEntities2 = new ArrayList<>();
        firstEntities2.add(first1);
        second2.setFirstEntities(firstEntities2);

        // Save entities
        FirstEntity savedFirst1 = firstRepository.save(first1);
        assertNotNull(savedFirst1);
        assertNotNull(savedFirst1.getId());

        FirstEntity savedFirst2 = firstRepository.save(first2);
        assertNotNull(savedFirst2);
        assertNotNull(savedFirst2.getId());

        SecondEntity savedSecond1 = secondRepository.save(second1);
        assertNotNull(savedSecond1);
        assertNotNull(savedSecond1.getId());

        SecondEntity savedSecond2 = secondRepository.save(second2);
        assertNotNull(savedSecond2);
        assertNotNull(savedSecond2.getId());

        // Verify relationships
        FirstEntity retrievedFirst1 = firstRepository.findById(savedFirst1.getId()).orElse(null);
        assertNotNull(retrievedFirst1);
        assertNotNull(retrievedFirst1.getSecondEntities());
        assertEquals(2, retrievedFirst1.getSecondEntities().size());

        FirstEntity retrievedFirst2 = firstRepository.findById(savedFirst2.getId()).orElse(null);
        assertNotNull(retrievedFirst2);
        assertNotNull(retrievedFirst2.getSecondEntities());
        assertEquals(1, retrievedFirst2.getSecondEntities().size());

        SecondEntity retrievedSecond1 = secondRepository.findById(savedSecond1.getId()).orElse(null);
        assertNotNull(retrievedSecond1);
        assertNotNull(retrievedSecond1.getFirstEntities());
        assertEquals(2, retrievedSecond1.getFirstEntities().size());

        SecondEntity retrievedSecond2 = secondRepository.findById(savedSecond2.getId()).orElse(null);
        assertNotNull(retrievedSecond2);
        assertNotNull(retrievedSecond2.getFirstEntities());
        assertEquals(1, retrievedSecond2.getFirstEntities().size());
    }

    @Entity
    public static class FirstEntity {
        @Id
        private Long id;
        private String name;
        @ManyToMany(entity = SecondEntity.class, mappedBy = "firstEntities")
        private List<SecondEntity> secondEntities;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<SecondEntity> getSecondEntities() {
            return secondEntities;
        }

        public void setSecondEntities(List<SecondEntity> secondEntities) {
            this.secondEntities = secondEntities;
        }
    }

    @Entity
    public static class SecondEntity {
        @Id
        private Long id;
        private String name;
        @ManyToMany(entity = FirstEntity.class, mappedBy = "secondEntities")
        private List<FirstEntity> firstEntities;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<FirstEntity> getFirstEntities() {
            return firstEntities;
        }

        public void setFirstEntities(List<FirstEntity> firstEntities) {
            this.firstEntities = firstEntities;
        }
    }
}
