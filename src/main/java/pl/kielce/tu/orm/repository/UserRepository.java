package pl.kielce.tu.orm.repository;

import pl.kielce.tu.orm.annotations.Repository;
import pl.kielce.tu.orm.entities.User;

import java.util.List;

@Repository(User.class)
public interface UserRepository extends CrudRepository<User, Long> {
    
    List<User> findByName(String name);
}