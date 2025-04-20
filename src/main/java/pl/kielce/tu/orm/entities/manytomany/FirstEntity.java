package pl.kielce.tu.orm.entities.manytomany;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToMany;

import java.util.List;

@Entity
public class FirstEntity {
    @Id
    private Long id;
    private String name;
    @ManyToMany(entity = SecondEntity.class, mappedBy = "firstEntities")
    private List<SecondEntity> secondEntities;
}
