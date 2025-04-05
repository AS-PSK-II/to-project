package pl.kielce.tu.orm.entities;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.OneToMany;

import java.util.List;

@Entity
public class Invoice {
    @Id
    private Long id;
    private String number;
    private String customer;
    private String company;
    @OneToMany(entity = Product.class, mappedBy = "invoice")
    private List<Product> products;

    public Invoice() {}

    public Invoice(Long id, String number, String customer, String company, List<Product> products) {
        this.id = id;
        this.number = number;
        this.customer = customer;
        this.company = company;
        this.products = products;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
