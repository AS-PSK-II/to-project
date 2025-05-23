package pl.kielce.tu.orm.annotations.processors.db.onetomany;

import pl.kielce.tu.orm.annotations.*;

@Entity
public class Child {
    @Id
    private Long id;
    private String name;
    @ManyToOne(entity = Parent.class, mappedBy = "children")
    private Parent parent;

    public Child() {}

    public Child(Long id, String name) {
        this.id = id;
        this.name = name;
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

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }
}
