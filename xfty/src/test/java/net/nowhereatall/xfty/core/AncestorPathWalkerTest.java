package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

/** Proves {@link AncestorPathWalker} - reading a field several relationship hops up a generated ancestor graph. */
class AncestorPathWalkerTest {

    private static final List<Field> ACCOUNT_NAME_PATH =
            List.of(Field.of(Contact.class, "accountId"), Field.of(Account.class, "name"));

    @Test
    void read_ForAOneHopAncestorFieldAndARow_ReadsTheAlignedParentValue() {
        // Arrange - two Contacts, each with its own parent Account carrying a distinct name
        Bundle bundle = new Bundle();
        bundle.putPrimaries(Field.of(Contact.class, "id"),
                new ArrayList<>(List.of(Contact.builder().build(), Contact.builder().build())));
        bundle.put(Field.of(Contact.class, "accountId"), new ArrayList<>(List.of(
                Account.builder().name("Row Zero").build(), Account.builder().name("Row One").build())));

        // Act
        Object rowOneName = AncestorPathWalker.read(bundle, ACCOUNT_NAME_PATH, 1);

        // Assert
        assertEquals("Row One", rowOneName);
    }

    @Test
    void read_ForAMultiHopPath_DescendsThroughEveryLeadingRelationship() {
        // Arrange - Contact -> Account (sub-bundle) -> parent Account (list), only the deepest name set
        Bundle accountBundle = new Bundle();
        accountBundle.put(Field.of(Account.class, "parentId"),
                new ArrayList<>(List.of(Account.builder().name("Grandparent").build())));
        Bundle bundle = new Bundle();
        bundle.put(Field.of(Contact.class, "accountId"), accountBundle);
        List<Field> path = List.of(
                Field.of(Contact.class, "accountId"), Field.of(Account.class, "parentId"), Field.of(Account.class, "name"));

        // Act
        Object grandparentName = AncestorPathWalker.read(bundle, path, 0);

        // Assert
        assertEquals("Grandparent", grandparentName);
    }

    @Test
    void read_WhenAHopWasNotGenerated_ReturnsNull() {
        // Arrange
        Bundle bundle = new Bundle();
        bundle.putPrimaries(Field.of(Contact.class, "id"), new ArrayList<>(List.of(Contact.builder().build())));

        // Act / Assert - an ungenerated ancestor reads as null, it does not throw
        assertNull(AncestorPathWalker.read(bundle, ACCOUNT_NAME_PATH, 0));
    }

    @Test
    void read_WhenTheRowIndexIsOutOfRange_ReturnsNull() {
        // Arrange
        Bundle bundle = new Bundle();
        bundle.put(Field.of(Contact.class, "accountId"), new ArrayList<>(List.of(Account.builder().name("Only").build())));

        // Act / Assert
        assertNull(AncestorPathWalker.read(bundle, ACCOUNT_NAME_PATH, 5));
    }

    @Test
    void read_WhenTheRowIndexIsNegative_ReturnsNull() {
        // Arrange
        Bundle bundle = new Bundle();
        bundle.put(Field.of(Contact.class, "accountId"), new ArrayList<>(List.of(Account.builder().name("Only").build())));

        // Act / Assert
        assertNull(AncestorPathWalker.read(bundle, ACCOUNT_NAME_PATH, -1));
    }

    @Test
    void read_WhenThePathIsTooShortToWalk_Throws() {
        // Arrange
        List<Field> justAField = List.of(Field.of(Account.class, "name"));

        // Act / Assert
        assertNotNull(assertThrows(XftyConfigurationException.class,
                () -> AncestorPathWalker.read(new Bundle(), justAField, 0)));
    }

    @Test
    void read_WhenAPathStepIsNull_Throws() {
        // Arrange
        List<Field> withNullStep = new ArrayList<>();
        withNullStep.add(Field.of(Contact.class, "accountId"));
        withNullStep.add(null);

        // Act / Assert
        assertNotNull(assertThrows(XftyConfigurationException.class,
                () -> AncestorPathWalker.read(new Bundle(), withNullStep, 0)));
    }
}
