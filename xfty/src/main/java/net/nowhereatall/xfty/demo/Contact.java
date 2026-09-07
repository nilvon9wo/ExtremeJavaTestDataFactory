package net.nowhereatall.xfty.demo;

import java.time.LocalDate;
import java.util.List;

/**
 * A Java record - the immutable half of the demo domain. Every place the engine
 * would reflectively set a field, it instead reconstructs a {@code Contact}
 * through this canonical constructor.
 *
 * <p>{@code account} / {@code cases} are navigation slots populated only by
 * enrichment. Use {@link #builder()} to construct one in a test without listing
 * every component.
 */
public record Contact(
        String id,
        String firstName,
        String lastName,
        String email,
        String accountId,
        String reportsToId,
        String department,
        LocalDate birthdate,
        Account account,
        List<Case> cases) {

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent construction for tests - each method names a component; {@link #build()} produces the record. */
    public static final class Builder {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String accountId;
        private String reportsToId;
        private String department;
        private LocalDate birthdate;
        private Account account;
        private List<Case> cases;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder reportsToId(String reportsToId) {
            this.reportsToId = reportsToId;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder birthdate(LocalDate birthdate) {
            this.birthdate = birthdate;
            return this;
        }

        public Builder account(Account account) {
            this.account = account;
            return this;
        }

        public Builder cases(List<Case> cases) {
            this.cases = cases;
            return this;
        }

        public Contact build() {
            return new Contact(this.id, this.firstName, this.lastName, this.email, this.accountId,
                    this.reportsToId, this.department, this.birthdate, this.account, this.cases);
        }
    }
}
