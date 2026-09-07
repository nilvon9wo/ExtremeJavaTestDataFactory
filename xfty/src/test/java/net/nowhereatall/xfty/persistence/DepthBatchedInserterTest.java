package net.nowhereatall.xfty.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Case;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link DepthBatchedInserter#resolveAll} - one pass per dependency
 * layer, of any mix of record types, either assigning mock ids or (with a real
 * gateway) inserting. Every scenario here uses {@code MOCK}, the same underlying
 * algorithm a real NOW insert runs.
 */
class DepthBatchedInserterTest {

    private static DepthBatchedInserterParentLink link(int childIndex, int parentIndex, Field field) {
        return new DepthBatchedInserterParentLink(childIndex, parentIndex, field);
    }

    @Test
    void resolveAll_ForNullLinksAndEmptyRecords_DoesNothing() {
        Async.await(DepthBatchedInserter.resolveAll(List.of(), null, InsertMode.MOCK));
    }

    @Test
    void resolveAll_ForIndependentRecords_AssignsEveryOneAnId() {
        // Arrange
        List<Object> records = new java.util.ArrayList<>(List.of(
                Account.builder().name("A").build(), Account.builder().name("B").build(), Account.builder().name("C").build()));

        // Act
        Async.await(DepthBatchedInserter.resolveAll(records, null, InsertMode.MOCK));

        // Assert
        assertNotNull(((Account) records.get(2)).getId());
    }

    @Test
    void resolveAll_ForAParentAndChild_ResolvesTheParentFirstAndPointsTheLookup() {
        // Arrange
        Account parent = Account.builder().name("Parent Co").build();
        Contact child = Contact.builder().lastName("Child").build();
        List<Object> records = new java.util.ArrayList<>(List.of(child, parent));

        // Act
        Async.await(DepthBatchedInserter.resolveAll(
                records, List.of(link(0, 1, Field.of(Contact.class, "accountId"))), InsertMode.MOCK));

        // Assert
        assertEquals(((Account) records.get(1)).getId(), ((Contact) records.get(0)).accountId());
    }

    @Test
    void resolveAll_ForParentsOfDifferentTypes_ResolvesThemAtTheSameLayer() {
        // Arrange - a Case needs both an Account and a Contact
        List<Object> records = new java.util.ArrayList<>(List.of(
                Case.builder().subject("Call").build(),
                Account.builder().name("What Co").build(),
                Contact.builder().lastName("Who").build()));

        // Act
        Async.await(DepthBatchedInserter.resolveAll(records, List.of(
                link(0, 1, Field.of(Case.class, "accountId")),
                link(0, 2, Field.of(Case.class, "contactId"))), InsertMode.MOCK));

        // Assert
        assertEquals(((Account) records.get(1)).getId(), ((Case) records.get(0)).getAccountId());
        assertEquals(((Contact) records.get(2)).id(), ((Case) records.get(0)).getContactId());
    }

    @Test
    void resolveAll_ForAChain_ResolvesOneLayerAtATime() {
        // Arrange
        List<Object> records = new java.util.ArrayList<>(List.of(
                Account.builder().name("Gen 1").build(),
                Contact.builder().lastName("Gen 2").build(),
                Contact.builder().lastName("Gen 3").build()));

        // Act
        Async.await(DepthBatchedInserter.resolveAll(records, List.of(
                link(1, 0, Field.of(Contact.class, "accountId")),
                link(2, 1, Field.of(Contact.class, "reportsToId"))), InsertMode.MOCK));

        // Assert
        assertEquals(((Account) records.get(0)).getId(), ((Contact) records.get(1)).accountId());
        assertEquals(((Contact) records.get(1)).id(), ((Contact) records.get(2)).reportsToId());
    }

    @Test
    void resolveAll_ForOneParentSharedByTwoChildren_ResolvesTheParentOnce() {
        // Arrange
        List<Object> records = new java.util.ArrayList<>(List.of(
                Account.builder().name("Shared").build(),
                Contact.builder().lastName("First").build(),
                Contact.builder().lastName("Second").build()));

        // Act
        Async.await(DepthBatchedInserter.resolveAll(records, List.of(
                link(1, 0, Field.of(Contact.class, "accountId")),
                link(2, 0, Field.of(Contact.class, "accountId"))), InsertMode.MOCK));

        // Assert
        assertEquals(((Account) records.get(0)).getId(), ((Contact) records.get(1)).accountId());
        assertEquals(((Account) records.get(0)).getId(), ((Contact) records.get(2)).accountId());
    }

    @Test
    void resolveAll_WhenTwoRecordsReferenceEachOther_Throws() {
        assertCyclic(
                new java.util.ArrayList<>(List.of(Account.builder().name("A").build(), Account.builder().name("B").build())),
                List.of(link(0, 1, Field.of(Account.class, "parentId")), link(1, 0, Field.of(Account.class, "parentId"))));
    }

    @Test
    void resolveAll_WhenARecordReferencesItself_Throws() {
        assertCyclic(
                new java.util.ArrayList<>(List.of(Account.builder().name("Loop").build())),
                List.of(link(0, 0, Field.of(Account.class, "parentId"))));
    }

    @Test
    void insertAll_AlwaysResolvesAsNow_WhichNeedsAGateway() {
        // Arrange
        List<Object> records = new java.util.ArrayList<>(List.of(Account.builder().name("A").build()));

        // Act
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> Async.await(DepthBatchedInserter.insertAll(records, null)));

        // Assert
        assertTrue(thrown.getMessage().contains("persistence gateway"));
    }

    private static void assertCyclic(List<Object> records, List<DepthBatchedInserterParentLink> links) {
        CyclicGraphException thrown = assertThrows(CyclicGraphException.class,
                () -> Async.await(DepthBatchedInserter.resolveAll(records, links, InsertMode.MOCK)));
        assertTrue(thrown.getMessage().contains("cycle"));
    }
}
