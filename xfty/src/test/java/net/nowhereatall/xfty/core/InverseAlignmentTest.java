package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.persistence.IdMocker;
import org.junit.jupiter.api.Test;

/** Proves {@link InverseAlignment} - for each parent, the children pointing back at it. Pure in-memory. */
class InverseAlignmentTest {

    private static final Field ACCOUNT_ID = Field.of(Contact.class, "accountId");

    @Test
    void childrenPerParent_WhenParentsHaveIds_MatchesOnTheForeignKey() {
        // Arrange
        String one = IdMocker.generateId();
        String two = IdMocker.generateId();
        List<Object> parents = List.of(Account.builder().id(one).build(), Account.builder().id(two).build());
        List<Object> children = List.of(
                Contact.builder().accountId(two).build(),
                Contact.builder().accountId(one).build(),
                Contact.builder().accountId(two).build());

        // Act
        List<List<Object>> perParent = InverseAlignment.childrenPerParent(parents, children, ACCOUNT_ID);

        // Assert
        assertEquals(1, perParent.get(0).size());
        assertEquals(2, perParent.get(1).size());
    }

    @Test
    void childrenPerParent_WhenParentsHaveNoIds_MatchesByPosition() {
        // Arrange
        List<Object> parents = List.of(new Account(), new Account());
        List<Object> children = List.of(
                Contact.builder().lastName("A").build(),
                Contact.builder().lastName("B").build());

        // Act
        List<List<Object>> perParent = InverseAlignment.childrenPerParent(parents, children, ACCOUNT_ID);

        // Assert
        assertEquals("A", ((Contact) perParent.get(0).get(0)).lastName());
        assertEquals("B", ((Contact) perParent.get(1).get(0)).lastName());
    }

    @Test
    void childrenPerParent_WhenNothingPointsAtAParent_GivesItAnEmptyList() {
        // Arrange
        List<Object> parents = List.of(Account.builder().id(IdMocker.generateId()).build());
        List<Object> children = List.of(Contact.builder().accountId(IdMocker.generateId()).build());

        // Act
        List<List<Object>> perParent = InverseAlignment.childrenPerParent(parents, children, ACCOUNT_ID);

        // Assert
        assertTrue(perParent.get(0).isEmpty());
    }

    @Test
    void childrenPerParent_WhenThereAreFewerChildrenThanParents_PositionFallbackGivesEmpty() {
        // Arrange
        List<Object> parents = List.of(new Account(), new Account());
        List<Object> children = List.of(Contact.builder().lastName("A").build());

        // Act
        List<List<Object>> perParent = InverseAlignment.childrenPerParent(parents, children, ACCOUNT_ID);

        // Assert
        assertEquals(1, perParent.get(0).size());
        assertTrue(perParent.get(1).isEmpty());
    }
}
