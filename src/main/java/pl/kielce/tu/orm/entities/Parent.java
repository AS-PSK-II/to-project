package pl.kielce.tu.orm.entities;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.OneToOne;

@Entity
public class Parent {
    @Id
    private Long id;
    private String name;
    @OneToOne(entity = Child.class)
    private Child child;

    public Parent() {}

    public Parent(Long id, String name, Child child) {
        this.id = id;
        this.name = name;
        this.child = child;
    }

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

    public Child getChild() {
        return child;
    }

    public void setChild(Child child) {
        this.child = child;
    }
}
