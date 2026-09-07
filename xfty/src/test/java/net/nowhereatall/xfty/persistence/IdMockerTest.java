package net.nowhereatall.xfty.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link IdMocker}, which fabricates a unique placeholder identifier
 * without touching a database. The mocked id is a plain "mock-N" string with no
 * per-type structure.
 */
class IdMockerTest {

    @Test
    void generateId_IsUniqueAcrossManyCalls() {
        // Arrange
        Set<String> generated = new HashSet<>();

        // Act
        for (int index = 0; index < 100; index++) {
            generated.add(IdMocker.generateId());
        }

        // Assert - every fabricated id is distinct
        assertEquals(100, generated.size());
    }

    @Test
    void addId_PopulatesTheIdFieldAndReturnsTheSameInstanceForAClass() {
        // Arrange
        Account record = Account.builder().name("Anything").build();

        // Act
        Object returned = IdMocker.addId(record, Field.of(Account.class, "id"));

        // Assert - a mutable class is updated in place
        assertSame(record, returned);
        assertNotNull(((Account) returned).getId());
    }

    @Test
    void addIds_PopulatesEveryRecordWithADistinctId() {
        // Arrange
        List<Object> records = List.of(
                Contact.builder().lastName("A").build(),
                Contact.builder().lastName("B").build(),
                Contact.builder().lastName("C").build());

        // Act - records are immutable, so the returned list carries the ids
        List<Object> withIds = IdMocker.addIds(records);

        // Assert
        Set<String> ids = new HashSet<>();
        for (Object record : withIds) {
            Contact contact = (Contact) record;
            assertNotNull(contact.id());
            ids.add(contact.id());
        }
        assertEquals(3, ids.size());
    }
}
