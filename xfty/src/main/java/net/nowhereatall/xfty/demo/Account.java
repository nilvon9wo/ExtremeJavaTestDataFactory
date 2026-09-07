package net.nowhereatall.xfty.demo;

import java.math.BigDecimal;
import java.util.List;

/**
 * A classic mutable class - the "bean" half of the demo domain. Reflection-based
 * field access reads and (for the engine) writes it identically to
 * {@link Contact}'s record shape.
 *
 * <p>{@code contacts} / {@code parent} / {@code childAccounts} are navigation
 * slots populated only by enrichment. {@link #getCreatedBy()} has no setter, so
 * the mutation-dispatch layer's backing-field write path is covered.
 */
public class Account {

    private String id;
    private String name;
    private String industry;
    private String type;
    private Integer numberOfEmployees;
    private BigDecimal annualRevenue;
    private String site;
    private String description;
    private String ownerId;
    private String parentId;
    private String accountNumber;
    private String shippingStreet;
    private String shippingCity;
    private String shippingCountry;
    private String billingStreet;
    private String billingCity;
    private boolean active;
    private String createdBy;

    private List<Contact> contacts;
    private Account parent;
    private List<Account> childAccounts;

    public static Builder builder() {
        return new Builder();
    }

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

    public BigDecimal getAnnualRevenue() {
        return this.annualRevenue;
    }

    public void setAnnualRevenue(BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    public String getSite() {
        return this.site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getParentId() {
        return this.parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getShippingStreet() {
        return this.shippingStreet;
    }

    public void setShippingStreet(String shippingStreet) {
        this.shippingStreet = shippingStreet;
    }

    public String getShippingCity() {
        return this.shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingCountry() {
        return this.shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public String getBillingStreet() {
        return this.billingStreet;
    }

    public void setBillingStreet(String billingStreet) {
        this.billingStreet = billingStreet;
    }

    public String getBillingCity() {
        return this.billingCity;
    }

    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
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

    public List<Contact> getContacts() {
        return this.contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Account getParent() {
        return this.parent;
    }

    public void setParent(Account parent) {
        this.parent = parent;
    }

    public List<Account> getChildAccounts() {
        return this.childAccounts;
    }

    public void setChildAccounts(List<Account> childAccounts) {
        this.childAccounts = childAccounts;
    }

    /** Fluent construction for tests. */
    public static final class Builder {
        private final Account account = new Account();

        public Builder id(String id) {
            this.account.id = id;
            return this;
        }

        public Builder name(String name) {
            this.account.name = name;
            return this;
        }

        public Builder industry(String industry) {
            this.account.industry = industry;
            return this;
        }

        public Builder type(String type) {
            this.account.type = type;
            return this;
        }

        public Builder numberOfEmployees(Integer numberOfEmployees) {
            this.account.numberOfEmployees = numberOfEmployees;
            return this;
        }

        public Builder annualRevenue(BigDecimal annualRevenue) {
            this.account.annualRevenue = annualRevenue;
            return this;
        }

        public Builder site(String site) {
            this.account.site = site;
            return this;
        }

        public Builder description(String description) {
            this.account.description = description;
            return this;
        }

        public Builder shippingCity(String shippingCity) {
            this.account.shippingCity = shippingCity;
            return this;
        }

        public Builder shippingCountry(String shippingCountry) {
            this.account.shippingCountry = shippingCountry;
            return this;
        }

        public Account build() {
            return this.account;
        }
    }
}
