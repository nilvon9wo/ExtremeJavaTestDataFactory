package net.nowhereatall.xfty.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

class RecordShapeTest {

    @Test
    void mutatesAClassInPlaceAndReturnsTheSameInstance() {
        // Arrange
        Account account = new Account();
        RecordShape shape = RecordShape.of(Account.class);

        // Act
        Object result = shape.set(account, Field.of(Account.class, "name"), "Acme");

        // Assert
        assertSame(account, result);
        assertEquals("Acme", account.getName());
    }

    @Test
    void writesAClassFieldThatHasNoSetterThroughItsBackingField() {
        // Arrange
        Account account = new Account();
        RecordShape shape = RecordShape.of(Account.class);

        // Act
        shape.set(account, Field.of(Account.class, "createdBy"), "seed-job");

        // Assert
        assertEquals("seed-job", account.getCreatedBy());
    }

    @Test
    void rebuildsARecordWithTheChangedComponentAndLeavesTheOriginalUntouched() {
        // Arrange
        Contact original = Contact.builder().id("c1").firstName("Ada").lastName("Lovelace").email("ada@example.com").accountId("a1").build();
        RecordShape shape = RecordShape.of(Contact.class);

        // Act
        Contact updated = (Contact) shape.set(original, Field.of(Contact.class, "email"), "ada@newmail.com");

        // Assert
        assertNotSame(original, updated);
        assertEquals("ada@example.com", original.email());
        assertEquals("ada@newmail.com", updated.email());
        assertEquals("Ada", updated.firstName());
    }

    @Test
    void appliesSeveralRecordComponentChangesInOneReconstruction() {
        // Arrange
        Contact original = Contact.builder().id("c1").firstName("Ada").lastName("Lovelace").email("ada@example.com").accountId("a1").build();
        RecordShape shape = RecordShape.of(Contact.class);

        // Act
        Contact updated = (Contact) shape.setAll(original, java.util.Map.of(
                Field.of(Contact.class, "firstName"), "Grace",
                Field.of(Contact.class, "lastName"), "Hopper"));

        // Assert
        assertEquals("Grace", updated.firstName());
        assertEquals("Hopper", updated.lastName());
        assertEquals("c1", updated.id());
    }

    @Test
    void copiesAClassIntoANewIndependentInstance() {
        // Arrange
        Account account = new Account();
        account.setName("Acme");
        account.setNumberOfEmployees(42);
        RecordShape shape = RecordShape.of(Account.class);

        // Act
        Account copy = (Account) shape.copy(account);

        // Assert
        assertNotSame(account, copy);
        assertEquals("Acme", copy.getName());
        assertEquals(42, copy.getNumberOfEmployees());
    }

    @Test
    void copyDoesNotShareMutationWithTheOriginal() {
        // Arrange
        Account account = new Account();
        account.setName("Acme");
        RecordShape shape = RecordShape.of(Account.class);
        Account copy = (Account) shape.copy(account);

        // Sanity Check
        assertEquals("Acme", copy.getName());

        // Act
        copy.setName("Globex");

        // Assert
        assertEquals("Acme", account.getName());
    }

    @Test
    void reportsWhetherATypeIsARecord() {
        // Arrange / Act / Assert
        assertTrue(RecordShape.of(Contact.class).isRecord());
        assertTrue(!RecordShape.of(Account.class).isRecord());
    }

    @Test
    void buildsABlankRecordWithComponentTypeDefaults() {
        // Arrange
        RecordShape shape = RecordShape.of(Contact.class);

        // Act
        Contact blank = (Contact) shape.instantiate();

        // Assert
        assertNull(blank.id());
        assertNull(blank.email());
    }

    @Test
    void listsRecordComponentsAsLogicalFields() {
        // Arrange
        RecordShape shape = RecordShape.of(Contact.class);

        // Act
        java.util.List<Field> fields = shape.fields();

        // Assert
        assertEquals(
                java.util.List.of("id", "firstName", "lastName", "email", "accountId", "reportsToId", "department", "birthdate", "account", "cases"),
                fields.stream().map(Field::name).toList());
    }
}
