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

class LookupKeyTest {

    @Test
    void internsOneInstancePerRecordType() {
        // Arrange / Act
        LookupKey first = LookupKey.get(Account.class);
        LookupKey second = LookupKey.get(Account.class);

        // Assert
        assertSame(first, second);
    }

    @Test
    void hasZeroSpecificity() {
        // Arrange / Act / Assert
        assertEquals(0, LookupKey.get(Account.class).specificity());
    }

    @Test
    void matchesAnInstanceOfExactlyItsType() {
        // Arrange
        LookupKey key = LookupKey.get(Contact.class);
        Contact contact = new Contact("c1", "Ada", "L", "a@b.com", "a1");

        // Act / Assert
        assertTrue(key.isInstanceOf(contact));
    }

    @Test
    void doesNotMatchADifferentType() {
        // Arrange
        LookupKey key = LookupKey.get(Contact.class);

        // Act / Assert
        assertFalse(key.isInstanceOf(new Account()));
    }

    @Test
    void rejectsANullRecordType() {
        // Act / Assert
        assertThrows(XftyConfigurationException.class, () -> LookupKey.get((Class<?>) null));
    }
}
