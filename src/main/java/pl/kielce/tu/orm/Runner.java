package pl.kielce.tu.orm;

import pl.kielce.tu.orm.initializer.DatabaseInitializer;

public class Runner {
    public static void main(String[] args) {
        DatabaseInitializer.initialize("jdbc:postgresql://localhost:5432/test", "postgres", "postgres", "org.postgresql.Driver");
    }
}
