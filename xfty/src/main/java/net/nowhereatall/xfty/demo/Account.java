package net.nowhereatall.xfty.demo;

import java.math.BigDecimal;

/**
 * A classic mutable class - the "bean" half of the demo domain, exercising
 * JavaBean getters/setters, a boolean {@code isX} accessor, and one getter with
 * no setter ({@link #getCreatedBy()}) so the mutation-dispatch layer's
 * backing-field write path is covered.
 */
public class Account {

    private String id;
    private String name;
    private String industry;
    private String type;
    private int numberOfEmployees;
    private BigDecimal annualRevenue;
    private boolean active;
    private String createdBy;

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

    public int getNumberOfEmployees() {
        return this.numberOfEmployees;
    }

    public void setNumberOfEmployees(int numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }

    public BigDecimal getAnnualRevenue() {
        return this.annualRevenue;
    }

    public void setAnnualRevenue(BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Deliberately has no setter - written only through the backing field. */
    public String getCreatedBy() {
        return this.createdBy;
    }
}
