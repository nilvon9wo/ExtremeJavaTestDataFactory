package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

class TypeLookupKeyTest {

    @Test
    void internsOneInstancePerRecordType() {
        // Arrange / Act
        TypeLookupKey first = TypeLookupKey.get(Account.class);
        TypeLookupKey second = TypeLookupKey.get(Account.class);

        // Assert
        assertSame(first, second);
    }

    @Test
    void hasZeroSpecificity() {
        // Arrange / Act / Assert
        assertEquals(0, TypeLookupKey.get(Account.class).specificity());
    }

    @Test
    void matchesAnInstanceOfExactlyItsType() {
        // Arrange
        TypeLookupKey key = TypeLookupKey.get(Contact.class);
        Contact contact = new Contact("c1", "Ada", "L", "a@b.com", "a1");

        // Act / Assert
        assertTrue(key.isInstanceOf(contact));
    }

    @Test
    void doesNotMatchADifferentType() {
        // Arrange
        TypeLookupKey key = TypeLookupKey.get(Contact.class);

        // Act / Assert
        assertFalse(key.isInstanceOf(new Account()));
    }

    @Test
    void rejectsANullRecordType() {
        // Act / Assert
        assertThrows(XftyConfigurationException.class, () -> TypeLookupKey.get((Class<?>) null));
    }
}
