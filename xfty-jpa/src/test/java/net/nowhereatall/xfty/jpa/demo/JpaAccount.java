package net.nowhereatall.xfty.jpa.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A JPA entity - a classic mutable class with a no-arg constructor, as the JPA
 * spec requires. The demo domain for the {@code xfty-jpa} tests is necessarily
 * entities like this; a Java record cannot be an {@code @Entity}. This does not
 * limit what XFTY's core generation supports, only what this one persistence
 * binding can target.
 */
@Entity
@Table(name = "jpa_account")
public class JpaAccount {

    @Id
    private String id;
    private String name;
    private String industry;
    private String type;
    private Integer numberOfEmployees;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return this.industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNumberOfEmployees() {
        return this.numberOfEmployees;
    }

    public void setNumberOfEmployees(Integer numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }
}
