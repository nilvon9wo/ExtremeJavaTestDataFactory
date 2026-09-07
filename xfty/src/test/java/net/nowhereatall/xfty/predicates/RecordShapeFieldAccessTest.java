package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

/**
 * Proves reflection-based field access ({@code Field}, the predicates) is
 * genuinely indifferent to how a record type declares its fields: {@link Account}'s
 * plain mutable-class properties and {@link Contact}'s record components both
 * work the same way, with no special-casing.
 */
class RecordShapeFieldAccessTest {

    @Test
    void isSatisfiedBy_AgainstAMutableClassProperty_ReadsItCorrectly() {
        // Arrange
        Account account = Account.builder().industry("Technology").build();
        RecordPredicateLike predicate = FieldPredicateFactory.equalTo(Account::getIndustry, "Technology");

        // Act
        boolean actualResult = predicate.isSatisfiedBy(account);

        // Assert
        assertTrue(actualResult);
    }

    @Test
    void isSatisfiedBy_AgainstARecordComponent_ReadsItCorrectly() {
        // Arrange
        Contact contact = Contact.builder().firstName("Ada").lastName("Lovelace").build();
        RecordPredicateLike predicate = FieldPredicateFactory.equalTo(Contact::lastName, "Lovelace");

        // Act
        boolean actualResult = predicate.isSatisfiedBy(contact);

        // Assert
        assertTrue(actualResult);
    }
}
