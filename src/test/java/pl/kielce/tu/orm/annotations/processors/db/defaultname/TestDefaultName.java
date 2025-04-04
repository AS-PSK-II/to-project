package pl.kielce.tu.orm.annotations.processors.db.defaultname;

import pl.kielce.tu.orm.annotations.Column;
import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;

@Entity
public class TestDefaultName {
    @Id
    private Long id;
    @Column(nullable = true, unique = true)
    private String name;
    private Integer age;

    public TestDefaultName() {
    }

    public TestDefaultName(Long id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
