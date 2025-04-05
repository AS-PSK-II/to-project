package pl.kielce.tu.orm.entities;

import pl.kielce.tu.orm.annotations.Entity;
import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToOne;

@Entity
public class Product {
    @Id
    private Long id;
    private String name;
    private Double price;
    @ManyToOne(entity = Invoice.class, mappedBy = "products")
    private Invoice invoice;

    public Product() {}

    public Product(Long id, String name, Double price, Invoice invoice) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.invoice = invoice;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
}
