package pl.kielce.tu.orm.entities.manytomany;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToMany;

import java.util.List;

@Entity
public class SecondEntity {
    @Id
    private Long id;
    private String name;
    @ManyToMany(entity = FirstEntity.class, mappedBy = "secondEntities")
    private List<FirstEntity> firstEntities;

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

    public List<FirstEntity> getFirstEntities() {
        return firstEntities;
    }

    public void setFirstEntities(List<FirstEntity> firstEntities) {
        this.firstEntities = firstEntities;
    }
}
