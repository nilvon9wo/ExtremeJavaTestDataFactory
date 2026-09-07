package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.AccountDataProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.ContactDataProvider;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.reflect.RecordShape;
import org.junit.jupiter.api.Test;

/**
 * Proves the {@link UnsetFieldFillerLike} hook: which fields count as "unset",
 * when the hook fires, and that it reaches generated ancestors too. A recording
 * test double keeps the contract independent of any one filler implementation.
 */
class UnsetFieldFillerTest {

    private static ProviderLookupLike lookup() {
        return ProviderLookups.of(Map.of(
                LookupKey.get(Account.class), new AccountDataProvider(),
                LookupKey.get(Contact.class), new ContactDataProvider()));
    }

    @Test
    void supply_WithNoFillerConfigured_LeavesUnconfiguredFieldsAtTheirDefault() {
        // Act
        Account result = Async.await(new RecordProvider<>(Account.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .supply());

        // Assert - AccountDataProvider's Master Template never puts numberOfEmployees
        assertNull(result.getNumberOfEmployees());
    }

    @Test
    void supply_WithAFillerConfigured_FillsOnlyFieldsTheMasterTemplateNeverConfigured() {
        // Arrange
        RecordingFiller filler = new RecordingFiller();

        // Act
        Async.await(new RecordProvider<>(Account.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setUnsetFieldFiller(filler)
                .supply());

        // Assert
        assertTrue(filler.fieldNamesSeen.contains("numberOfEmployees"));
        assertTrue(filler.fieldNamesSeen.contains("annualRevenue"));
        assertFalse(filler.fieldNamesSeen.contains("id"));
        assertFalse(filler.fieldNamesSeen.contains("name"));
        assertFalse(filler.fieldNamesSeen.contains("industry"));
    }

    @Test
    void supply_TheFilledValue_SurvivesIntoTheReturnedRecord() {
        // Arrange
        SettingFiller filler = new SettingFiller("numberOfEmployees", 42);

        // Act
        Account result = Async.await(new RecordProvider<>(Account.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setUnsetFieldFiller(filler)
                .supply());

        // Assert
        assertEquals(42, result.getNumberOfEmployees());
    }

    @Test
    void supply_ForARequiredRelationship_AppliesTheSameFillerToTheGeneratedAncestorToo() {
        // Arrange
        RecordingFiller filler = new RecordingFiller();

        // Act
        Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setUnsetFieldFiller(filler)
                .supply());

        // Assert
        assertTrue(filler.recordTypesSeen.contains(Contact.class));
        assertTrue(filler.recordTypesSeen.contains(Account.class));
    }

    @Test
    void supply_ARelationshipsOwnScalarField_IsNeverTreatedAsUnset() {
        // Arrange - accountId is a required relationship field, not a "nothing touched it" field
        RecordingFiller filler = new RecordingFiller();

        // Act
        Async.await(new RecordProvider<>(Contact.class, lookup())
                .setInsertMode(InsertMode.MOCK)
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setUnsetFieldFiller(filler)
                .supply());

        // Assert
        assertFalse(filler.fieldNamesSeen.contains("accountId"));
    }

    private static final class RecordingFiller implements UnsetFieldFillerLike {
        private final List<Class<?>> recordTypesSeen = new ArrayList<>();
        private final List<String> fieldNamesSeen = new ArrayList<>();

        @Override
        public Object fill(Object record, Collection<Field> unsetFields) {
            this.recordTypesSeen.add(record.getClass());
            for (Field field : unsetFields) {
                this.fieldNamesSeen.add(field.name());
            }
            return record;
        }
    }

    private static final class SettingFiller implements UnsetFieldFillerLike {
        private final String fieldName;
        private final Object value;

        private SettingFiller(String fieldName, Object value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public Object fill(Object record, Collection<Field> unsetFields) {
            for (Field field : unsetFields) {
                if (field.name().equals(this.fieldName)) {
                    return RecordShape.of(record.getClass()).set(record, field, this.value);
                }
            }
            return record;
        }
    }
}
