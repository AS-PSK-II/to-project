package pl.kielce.tu.orm.annotations.processors.db.manytomany;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToMany;

import java.util.List;

@Entity
public class SecondEntity {
    @Id
    private Long id;
    private String name;
    @ManyToMany(entity = FirstEntity.class, mappedBy = "entities")
    private List<FirstEntity> entities;
}
