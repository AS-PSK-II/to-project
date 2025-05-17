package pl.kielce.tu.orm;

import pl.kielce.tu.orm.entities.User;
import pl.kielce.tu.orm.initializer.DatabaseInitializer;
import pl.kielce.tu.orm.repository.RepositoryFactory;
import pl.kielce.tu.orm.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class Runner {
    public static void main(String[] args) {
        DatabaseInitializer.initialize("jdbc:postgresql://localhost:5432/test", "postgres", "postgres", "org.postgresql.Driver");

        RepositoryFactory repositoryFactory = RepositoryFactory.getInstance();

        demonstrateUserRepository(repositoryFactory);
    }

    private static void demonstrateUserRepository(RepositoryFactory repositoryFactory) {
        System.out.println("=== Demonstrating User Repository ===");

        UserRepository userRepository = repositoryFactory.getRepository(UserRepository.class);

        User user1 = new User(1L, "John Doe", "john@example.com", "password123");
        User user2 = new User(2L, "Jane Smith", "jane@example.com", "password456");
        User user3 = new User(3L, "John Smith", "john.smith@example.com", "password789");

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        System.out.println("Saved users: " + userRepository.count());

        Optional<User> foundUser = userRepository.findById(1L);
        foundUser.ifPresent(user -> System.out.println("Found user by ID: " + user));

        List<User> johnUsers = userRepository.findByName("John Doe");
        System.out.println("Found users by name 'John Doe': " + johnUsers.size());
        johnUsers.forEach(System.out::println);

        List<User> allUsers = userRepository.findAll();
        System.out.println("All users: " + allUsers.size());
        allUsers.forEach(System.out::println);

        if (foundUser.isPresent()) {
            User user = foundUser.get();
            user.setEmail("john.updated@example.com");
            userRepository.save(user);
            System.out.println("Updated user: " + userRepository.findById(1L).orElse(null));
        }

        userRepository.deleteById(3L);
        System.out.println("After deleting user with ID 3, count: " + userRepository.count());

        System.out.println();
    }
}
