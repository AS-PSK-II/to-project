package pl.kielce.tu.orm.annotations.processors.db.onetoone;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.OneToOne;

@Entity
public class Child {
    @Id
    private Long id;
    private String name;
    @OneToOne(child = Parent.class)
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
