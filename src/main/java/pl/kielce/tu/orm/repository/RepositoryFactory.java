package pl.kielce.tu.orm.repository;

import pl.kielce.tu.orm.annotations.Repository;
import pl.kielce.tu.orm.repository.impl.CrudRepositoryImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class RepositoryFactory {
    private static final RepositoryFactory instance = new RepositoryFactory();
    private final Map<Class<?>, Object> repositories = new HashMap<>();

    private RepositoryFactory() {
    }

    public static RepositoryFactory getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> CrudRepository<T, ID> getRepositoryForEntity(Class<T> entityClass) {
        if (repositories.containsKey(entityClass)) {
            return (CrudRepository<T, ID>) repositories.get(entityClass);
        }

        CrudRepositoryImpl<T, ID> repository = new CrudRepositoryImpl<>(entityClass);
        repositories.put(entityClass, repository);
        return repository;
    }

    @SuppressWarnings("unchecked")
    public <T, ID, R extends CrudRepository<T, ID>> R getRepository(Class<R> repositoryInterface) {
        if (!repositoryInterface.isAnnotationPresent(Repository.class)) {
            throw new IllegalArgumentException("Interface " + repositoryInterface.getName() + " is not a repository");
        }

        Repository annotation = repositoryInterface.getAnnotation(Repository.class);
        Class<T> entityClass = (Class<T>) annotation.value();

        if (repositories.containsKey(repositoryInterface)) {
            return (R) repositories.get(repositoryInterface);
        }

        // Special case for UserRepository
        if (repositoryInterface.equals(pl.kielce.tu.orm.repository.UserRepository.class)) {
            pl.kielce.tu.orm.repository.impl.UserRepositoryImpl repository = new pl.kielce.tu.orm.repository.impl.UserRepositoryImpl();
            repositories.put(repositoryInterface, repository);
            return (R) repository;
        }

        if (repositoryInterface.equals(CrudRepository.class)) {
            CrudRepositoryImpl<T, ID> repository = new CrudRepositoryImpl<>(entityClass);
            repositories.put(repositoryInterface, repository);
            return (R) repository;
        }

        try {
            String implClassName = repositoryInterface.getName() + "Impl";
            Class<?> implClass = Class.forName(implClassName);

            try {
                Constructor<?> constructor = implClass.getConstructor(Class.class);
                R repository = (R) constructor.newInstance(entityClass);
                repositories.put(repositoryInterface, repository);
                return repository;
            } catch (NoSuchMethodException e) {
                Constructor<?> constructor = implClass.getConstructor();
                R repository = (R) constructor.newInstance();
                repositories.put(repositoryInterface, repository);
                return repository;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            CrudRepositoryImpl<T, ID> repository = new CrudRepositoryImpl<>(entityClass);
            repositories.put(repositoryInterface, repository);
            return (R) repository;
        }
    }
}
