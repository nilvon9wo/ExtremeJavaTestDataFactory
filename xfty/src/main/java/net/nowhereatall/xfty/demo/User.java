package net.nowhereatall.xfty.demo;

/**
 * A minimal demo type - a self-referencing {@code managerId} - to exercise
 * deep/hierarchical relationship paths and multi-variant Provider chains.
 */
public class User {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String managerId;

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

    public String getManagerId() {
        return this.managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }
}
