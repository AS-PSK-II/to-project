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
