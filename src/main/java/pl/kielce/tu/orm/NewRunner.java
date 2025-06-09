package pl.kielce.tu.orm;

import pl.kielce.tu.orm.dialects.PostgreSQLDialect;
import pl.kielce.tu.orm.entities.Child;
import pl.kielce.tu.orm.entities.Invoice;
import pl.kielce.tu.orm.entities.Parent;
import pl.kielce.tu.orm.entities.Product;
import pl.kielce.tu.orm.entities.User;
import pl.kielce.tu.orm.entities.manytomany.FirstEntity;
import pl.kielce.tu.orm.entities.manytomany.SecondEntity;
import pl.kielce.tu.orm.initializer.DatabaseInitializer;
import pl.kielce.tu.orm.repository.CrudRepository;
import pl.kielce.tu.orm.repository.RepositoryFactory;
import pl.kielce.tu.orm.repository.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * New runner class to demonstrate the ORM functionality.
 */
public class NewRunner {
    public static void main(String[] args) {
        // Initialize the database
        DatabaseInitializer.initialize("jdbc:postgresql://localhost:5432/test", "postgres", "postgres", "org.postgresql.Driver", new PostgreSQLDialect());
        
        // Get the repository factory
        RepositoryFactory repositoryFactory = RepositoryFactory.getInstance();
        
        // Demonstrate User repository
        demonstrateUserRepository(repositoryFactory);
        
        // Demonstrate Parent-Child relationship (OneToOne)
        demonstrateOneToOneRelationship(repositoryFactory);
        
        // Demonstrate Invoice-Product relationship (OneToMany/ManyToOne)
        demonstrateOneToManyRelationship(repositoryFactory);
        
        // Demonstrate FirstEntity-SecondEntity relationship (ManyToMany)
        demonstrateManyToManyRelationship(repositoryFactory);
    }
    
    private static void demonstrateUserRepository(RepositoryFactory repositoryFactory) {
        System.out.println("=== Demonstrating User Repository ===");
        
        // Get the user repository
        UserRepository userRepository = repositoryFactory.getRepository(UserRepository.class);
        
        // Create and save users
        User user1 = new User(1L, "John Doe", "john@example.com", "password123");
        User user2 = new User(2L, "Jane Smith", "jane@example.com", "password456");
        User user3 = new User(3L, "John Smith", "john.smith@example.com", "password789");
        
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        
        System.out.println("Saved users: " + userRepository.count());
        
        // Find users by ID
        Optional<User> foundUser = userRepository.findById(1L);
        foundUser.ifPresent(user -> System.out.println("Found user by ID: " + user));
        
        // Find users by name
        List<User> johnUsers = userRepository.findByName("John Doe");
        System.out.println("Found users by name 'John Doe': " + johnUsers.size());
        johnUsers.forEach(System.out::println);
        
        // Find all users
        List<User> allUsers = userRepository.findAll();
        System.out.println("All users: " + allUsers.size());
        allUsers.forEach(System.out::println);
        
        // Update a user
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            user.setEmail("john.updated@example.com");
            userRepository.save(user);
            System.out.println("Updated user: " + userRepository.findById(1L).orElse(null));
        }
        
        // Delete a user
        userRepository.deleteById(3L);
        System.out.println("After deleting user with ID 3, count: " + userRepository.count());
        
        System.out.println();
    }
    
    private static void demonstrateOneToOneRelationship(RepositoryFactory repositoryFactory) {
        System.out.println("=== Demonstrating One-to-One Relationship ===");
        
        // Get repositories
        CrudRepository<Parent, Long> parentRepository = repositoryFactory.getRepositoryForEntity(Parent.class);
        CrudRepository<Child, Long> childRepository = repositoryFactory.getRepositoryForEntity(Child.class);
        
        // Create and save entities
        Child child = new Child(1L, "Child 1");
        Parent parent = new Parent(1L, "Parent 1", null);

        parentRepository.save(parent);
        childRepository.save(child);

        child.setParent(parent);
        parent.setChild(child);

        parentRepository.save(parent);
        childRepository.save(child);
        
        // Find entities
        Optional<Parent> foundParent = parentRepository.findById(1L);
        foundParent.ifPresent(p -> {
            System.out.println("Found parent: " + p.getName());
            if (p.getChild() != null) {
                System.out.println("Parent's child: " + p.getChild().getName());
            }
        });
        
        Optional<Child> foundChild = childRepository.findById(1L);
        foundChild.ifPresent(c -> {
            System.out.println("Found child: " + c.getName());
            if (c.getParent() != null) {
                System.out.println("Child's parent: " + c.getParent().getName());
            }
        });
        
        System.out.println();
    }
    
    private static void demonstrateOneToManyRelationship(RepositoryFactory repositoryFactory) {
        System.out.println("=== Demonstrating One-to-Many Relationship ===");
        
        // Get repositories
        CrudRepository<Invoice, Long> invoiceRepository = repositoryFactory.getRepositoryForEntity(Invoice.class);
        CrudRepository<Product, Long> productRepository = repositoryFactory.getRepositoryForEntity(Product.class);
        
        // Create and save entities
        Invoice invoice = new Invoice(1L, "INV-001", "Customer 1", "Company 1", new ArrayList<>());
        
        Product product1 = new Product(1L, "Product 1", 10.0, invoice);
        Product product2 = new Product(2L, "Product 2", 20.0, invoice);
        
        invoice.setProducts(Arrays.asList(product1, product2));
        
        invoiceRepository.save(invoice);
        
        // Find entities
        Optional<Invoice> foundInvoice = invoiceRepository.findById(1L);
        foundInvoice.ifPresent(i -> {
            System.out.println("Found invoice: " + i.getNumber());
            if (i.getProducts() != null) {
                System.out.println("Invoice products: " + i.getProducts().size());
                i.getProducts().forEach(p -> System.out.println("- " + p.getName() + ": $" + p.getPrice()));
            }
        });
        
        Optional<Product> foundProduct = productRepository.findById(1L);
        foundProduct.ifPresent(p -> {
            System.out.println("Found product: " + p.getName());
            if (p.getInvoice() != null) {
                System.out.println("Product's invoice: " + p.getInvoice().getNumber());
            }
        });
        
        System.out.println();
    }
    
    private static void demonstrateManyToManyRelationship(RepositoryFactory repositoryFactory) {
        System.out.println("=== Demonstrating Many-to-Many Relationship ===");
        
        // Get repositories
        CrudRepository<FirstEntity, Long> firstEntityRepository = repositoryFactory.getRepositoryForEntity(FirstEntity.class);
        CrudRepository<SecondEntity, Long> secondEntityRepository = repositoryFactory.getRepositoryForEntity(SecondEntity.class);
        
        // Create and save entities
        FirstEntity first1 = new FirstEntity();
        first1.setId(1L);
        first1.setName("First Entity 1");
        
        FirstEntity first2 = new FirstEntity();
        first2.setId(2L);
        first2.setName("First Entity 2");
        
        SecondEntity second1 = new SecondEntity();
        second1.setId(1L);
        second1.setName("Second Entity 1");
        
        SecondEntity second2 = new SecondEntity();
        second2.setId(2L);
        second2.setName("Second Entity 2");
        
        // Set up relationships
        List<SecondEntity> secondEntities1 = new ArrayList<>();
        secondEntities1.add(second1);
        first1.setSecondEntities(secondEntities1);
        
        List<SecondEntity> secondEntities2 = new ArrayList<>();
        secondEntities2.add(second1);
        secondEntities2.add(second2);
        first2.setSecondEntities(secondEntities2);
        
        List<FirstEntity> firstEntities1 = new ArrayList<>();
        firstEntities1.add(first1);
        firstEntities1.add(first2);
        second1.setFirstEntities(firstEntities1);
        
        List<FirstEntity> firstEntities2 = new ArrayList<>();
        firstEntities2.add(first2);
        second2.setFirstEntities(firstEntities2);
        
        // Save entities
        firstEntityRepository.save(first1);
        firstEntityRepository.save(first2);
        
        // Find entities
        Optional<FirstEntity> foundFirst = firstEntityRepository.findById(1L);
        foundFirst.ifPresent(f -> {
            System.out.println("Found first entity: " + f.getName());
            if (f.getSecondEntities() != null) {
                System.out.println("First entity's second entities: " + f.getSecondEntities().size());
                f.getSecondEntities().forEach(s -> System.out.println("- " + s.getName()));
            }
        });
        
        Optional<SecondEntity> foundSecond = secondEntityRepository.findById(1L);
        foundSecond.ifPresent(s -> {
            System.out.println("Found second entity: " + s.getName());
            if (s.getFirstEntities() != null) {
                System.out.println("Second entity's first entities: " + s.getFirstEntities().size());
                s.getFirstEntities().forEach(f -> System.out.println("- " + f.getName()));
            }
        });
        
        System.out.println();
    }
}