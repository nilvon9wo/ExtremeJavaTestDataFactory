package net.nowhereatall.xfty.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import org.junit.jupiter.api.Test;

/**
 * Proves ancestor-cycle detection in {@link AncestorGenerator}: a self-referential
 * relationship generates one level, a deeper same-key chain throws, and
 * {@code allowAncestorCycles()} suppresses the guard.
 */
class AncestorCycleTest {

    private static final Field REPORTS_TO_ID = Field.of(Contact.class, "reportsToId");

    private static ProviderLookupLike selfReferringLookup() {
        return ProviderLookups.of(Map.of(LookupKey.get(Contact.class), new SelfReferringContactProvider()));
    }

    @Test
    void supplyBundle_WithOneLevelOfSelfReference_StopsTheChainOnItsOwn() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, selfReferringLookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .includeOptional(REPORTS_TO_ID)
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        // Sanity Check
        assertNotNull(bundle.getBundle(Contact.class, "reportsToId"));

        // Assert - the manager Contact does not get its own manager, the chain stops
        assertNull(bundle.getBundle(Contact.class, "reportsToId").getBundle(Contact.class, "reportsToId"));
    }

    @Test
    void supplyBundle_WhenAForcedPathRepeatsTheSameRelationshipKey_Throws() {
        // Arrange
        RecordProvider<Contact> provider = new RecordProvider<>(Contact.class, selfReferringLookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .includeOptional(List.of(REPORTS_TO_ID, REPORTS_TO_ID))
                .setInsertMode(InsertMode.MOCK);

        // Act
        XftyConfigurationException thrown =
                assertThrows(XftyConfigurationException.class, () -> Async.await(provider.supplyBundle()));

        // Assert
        assertTrue(thrown.getMessage().contains("cycle"));
        assertTrue(thrown.getMessage().contains("reportsToId"));
    }

    @Test
    void supplyBundle_WhenAllowAncestorCyclesIsSet_BuildsTheForcedDeepChainThenStops() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, selfReferringLookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .includeOptional(List.of(REPORTS_TO_ID, REPORTS_TO_ID))
                .allowAncestorCycles()
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        // Assert
        Bundle levelTwo = bundle.getBundle(Contact.class, "reportsToId").getBundle(Contact.class, "reportsToId");
        assertNotNull(levelTwo);
        assertNull(levelTwo.getBundle(Contact.class, "reportsToId"));
    }

    static final class SelfReferringContactProvider extends SimpleRecordProvider {
        SelfReferringContactProvider() {
            super(MasterTemplate.of(Contact::id)
                    .put(Contact::lastName, new IncrementingStringExpression("Mgr"))
                    .putOptional(Contact::reportsToId, new DefaultRelationship(Contact.builder().build())));
        }
    }
}
