package net.nowhereatall.xfty.jpa.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A JPA entity; {@code accountId} is a plain foreign-key column XFTY wires to a generated {@link JpaAccount}. */
@Entity
@Table(name = "jpa_contact")
public class JpaContact {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String accountId;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
