package net.nowhereatall.xfty.demo;

/**
 * A third demo type, for a genuine three-level hierarchy (Account → Contact →
 * Case) in downward-generation and deep-path tests.
 */
public class Case {

    private String id;
    private String subject;
    private String origin;
    private String accountId;
    private String contactId;

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOrigin() {
        return this.origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getContactId() {
        return this.contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public static final class Builder {
        private final Case value = new Case();

        public Builder id(String id) {
            this.value.id = id;
            return this;
        }

        public Builder subject(String subject) {
            this.value.subject = subject;
            return this;
        }

        public Builder origin(String origin) {
            this.value.origin = origin;
            return this;
        }

        public Builder accountId(String accountId) {
            this.value.accountId = accountId;
            return this;
        }

        public Builder contactId(String contactId) {
            this.value.contactId = contactId;
            return this;
        }

        public Case build() {
            return this.value;
        }
    }
}
