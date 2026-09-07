package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

/** Proves {@link BundleMerger} - folding the sibling child bundles of one relationship field into a single navigable bundle. */
class BundleMergerTest {

    @Test
    void combine_Always_ConcatenatesEveryBundlesPrimariesInDeclarationOrder() {
        // Arrange
        Bundle first = contactBundle(List.of(Contact.builder().lastName("A").build()));
        Bundle second = contactBundle(List.of(
                Contact.builder().lastName("B").build(), Contact.builder().lastName("C").build()));

        // Act
        Bundle merged = BundleMerger.combine(List.of(first, second));

        // Assert
        List<Object> primaries = merged.primaryRecords();
        assertEquals(3, primaries.size());
        assertEquals("A", ((Contact) primaries.get(0)).lastName());
        assertEquals("C", ((Contact) primaries.get(2)).lastName());
    }

    @Test
    void combine_WhenABundleHasNoPrimaries_SkipsItAndKeepsTheRest() {
        // Arrange
        Bundle empty = new Bundle();
        empty.putPrimaries(Field.of(Contact.class, "id"), new ArrayList<>());
        Bundle populated = contactBundle(List.of(Contact.builder().lastName("Only").build()));

        // Act
        Bundle merged = BundleMerger.combine(List.of(empty, populated));

        // Assert
        assertEquals(1, merged.primaryRecords().size());
    }

    @Test
    void combine_WhenTheFirstBundleHasNoPrimaryTargetField_PutsNoPrimaries() {
        // Arrange
        Bundle noField = new Bundle();
        Bundle withField = contactBundle(List.of(Contact.builder().lastName("Ignored").build()));

        // Act
        Bundle merged = BundleMerger.combine(List.of(noField, withField));

        // Assert
        assertNull(merged.primaryRecords());
    }

    @Test
    void combine_WhenAParentFieldIsInOneBundleOnly_CarriesThatSubBundleThrough() {
        // Arrange
        Bundle child = contactBundle(List.of(Contact.builder().lastName("Child").build()));
        child.put(Field.of(Contact.class, "accountId"), accountBundle(List.of(Account.builder().name("Parent").build())));
        Bundle plain = contactBundle(List.of(Contact.builder().lastName("Sibling").build()));

        // Act
        Bundle merged = BundleMerger.combine(List.of(child, plain));

        // Assert
        List<Object> parents = merged.getBundle(Contact.class, "accountId").primaryRecords();
        assertEquals(1, parents.size());
        assertEquals("Parent", ((Account) parents.get(0)).getName());
    }

    @Test
    void combine_WhenAParentFieldIsInBothBundles_CombinesTheirParentPrimaries() {
        // Arrange
        Bundle first = contactBundle(List.of(Contact.builder().lastName("One").build()));
        first.put(Field.of(Contact.class, "accountId"), accountBundle(List.of(Account.builder().name("Acme").build())));
        Bundle second = contactBundle(List.of(Contact.builder().lastName("Two").build()));
        second.put(Field.of(Contact.class, "accountId"), accountBundle(List.of(Account.builder().name("Globex").build())));

        // Act
        Bundle merged = BundleMerger.combine(List.of(first, second));

        // Assert
        assertEquals(2, merged.getBundle(Contact.class, "accountId").primaryRecords().size());
    }

    private static Bundle contactBundle(List<Object> contacts) {
        Bundle bundle = new Bundle();
        bundle.putPrimaries(Field.of(Contact.class, "id"), new ArrayList<>(contacts));
        return bundle;
    }

    private static Bundle accountBundle(List<Object> accounts) {
        Bundle bundle = new Bundle();
        bundle.putPrimaries(Field.of(Account.class, "id"), new ArrayList<>(accounts));
        return bundle;
    }
}
