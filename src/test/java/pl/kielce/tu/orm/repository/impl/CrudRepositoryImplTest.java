package pl.kielce.tu.orm.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.OneToOne;
import pl.kielce.tu.orm.connector.DatabaseConnector;
import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.initializer.DatabaseInitializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CrudRepositoryImplTest {

    private CrudRepositoryImpl<ParentEntity, Long> parentRepository;
    private CrudRepositoryImpl<ChildEntity, Long> childRepository;
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
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS child_entity")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS parent_entity")) {
            statement.executeUpdate();
        }

        // Create repositories
        parentRepository = new CrudRepositoryImpl<>(ParentEntity.class);
        childRepository = new CrudRepositoryImpl<>(ChildEntity.class);
    }

    @Test
    @Disabled
    void testOneToOneRelationship() {
        // Create parent and child
        ParentEntity parent = new ParentEntity();
        parent.setId(1L);
        parent.setName("Parent 1");

        ChildEntity child = new ChildEntity();
        child.setId(1L);
        child.setName("Child 1");

        // Set relationships
        parent.setChild(child);
        child.setParent(parent);

        // Save parent first
        ParentEntity savedParent = parentRepository.save(parent);
        assertNotNull(savedParent);
        assertNotNull(savedParent.getId());

        // Save child
        ChildEntity savedChild = childRepository.save(child);
        assertNotNull(savedChild);
        assertNotNull(savedChild.getId());
        assertNotNull(savedChild.getParent());

        // Verify relationships
        ParentEntity retrievedParent = parentRepository.findById(savedParent.getId()).orElse(null);
        assertNotNull(retrievedParent);
        assertNotNull(retrievedParent.getChild());

        ChildEntity retrievedChild = childRepository.findById(savedChild.getId()).orElse(null);
        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getParent());
    }

    @Entity
    public static class ParentEntity {
        @Id
        private Long id;
        private String name;
        @OneToOne(entity = ChildEntity.class)
        private ChildEntity child;

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

        public ChildEntity getChild() {
            return child;
        }

        public void setChild(ChildEntity child) {
            this.child = child;
        }
    }

    @Entity
    public static class ChildEntity {
        @Id
        private Long id;
        private String name;
        @OneToOne(entity = ParentEntity.class)
        private ParentEntity parent;

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

        public ParentEntity getParent() {
            return parent;
        }

        public void setParent(ParentEntity parent) {
            this.parent = parent;
        }
    }
}
