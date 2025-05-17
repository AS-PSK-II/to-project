package pl.kielce.tu.orm.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {
    
    T save(T entity);
    
    List<T> saveAll(Iterable<T> entities);
    
    Optional<T> findById(ID id);
    
    boolean existsById(ID id);
    
    List<T> findAll();
    
    List<T> findAllById(Iterable<ID> ids);
    
    long count();
    
    void deleteById(ID id);
    
    void delete(T entity);
    
    void deleteAllById(Iterable<ID> ids);
    
    void deleteAll(Iterable<T> entities);
    
    void deleteAll();
}