package pl.kielce.tu.orm.annotations.processors.db.onetomany;

import pl.kielce.tu.orm.annotations.*;

import java.util.Set;

@Entity
public class Parent {
    @Id
    private Long id;
    private String name;
    @OneToMany(entity = Child.class, mappedBy = "parent")
    private Set<Child> children;

    public Parent() {}

    public Parent(Long id, String name, Set<Child> children) {
        this.id = id;
        this.name = name;
        this.children = children;
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

    public Set<Child> getChildren() {
        return children;
    }

    public void setChildren(Set<Child> children) {
        this.children = children;
    }
}
