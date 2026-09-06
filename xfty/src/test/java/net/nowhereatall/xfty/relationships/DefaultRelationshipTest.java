package net.nowhereatall.xfty.relationships;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.lookup.FlavouredLookupKey;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.predicates.FieldPredicateFactory;
import org.junit.jupiter.api.Test;

class DefaultRelationshipTest {

    private static final class StubProvider implements RecordProviderLike {
        @Override
        public Class<?> primaryType() {
            return Account.class;
        }
    }

    private static ProviderLookupLike lookupWith(Map<LookupKeyLike, RecordProviderLike> providers) {
        return ProviderLookups.of(providers);
    }

    @Test
    void exposesItsOverrideTemplateAndRelatedField() {
        // Arrange
        Account template = new Account();
        Field relatedField = Field.of(Account.class, "id");

        // Act
        DefaultRelationship relationship = new DefaultRelationship(template, relatedField);

        // Assert
        assertSame(template, relationship.overrideTemplate());
        assertEquals(relatedField, relationship.relatedField());
    }

    @Test
    void relatedFieldIsNullWhenTheParentIdShouldBeUsed() {
        // Arrange / Act
        DefaultRelationship relationship = new DefaultRelationship(new Account());

        // Assert
        assertNull(relationship.relatedField());
    }

    @Test
    void resolvesAnExplicitLookupKeyAsGiven() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "rel-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        ProviderLookupLike lookup = lookupWith(Map.of(techKey, new StubProvider()));
        DefaultRelationship relationship = new DefaultRelationship(techKey, new Account());

        // Act
        LookupKeyLike resolved = relationship.resolveLookupKey(lookup);

        // Assert
        assertEquals(techKey.hashKey(), resolved.hashKey());
    }

    @Test
    void derivesTheLookupKeyFromTheOverrideTemplateWhenNoExplicitKeyIsGiven() {
        // Arrange
        FlavouredLookupKey techKey = FlavouredLookupKey.get(Account.class, "rel-derive-tech")
                .matching(FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));
        ProviderLookupLike lookup = lookupWith(Map.of(
                LookupKey.get(Account.class), new StubProvider(),
                techKey, new StubProvider()));
        Account techTemplate = new Account();
        techTemplate.setIndustry("Technology");
        DefaultRelationship relationship = new DefaultRelationship(techTemplate);

        // Act
        LookupKeyLike resolved = relationship.resolveLookupKey(lookup);

        // Assert
        assertEquals(techKey.hashKey(), resolved.hashKey());
    }

    @Test
    void memoisesTheResolvedKey() {
        // Arrange
        ProviderLookupLike lookup = lookupWith(Map.of(LookupKey.get(Account.class), new StubProvider()));
        DefaultRelationship relationship = new DefaultRelationship(new Account());

        // Act
        LookupKeyLike first = relationship.resolveLookupKey(lookup);
        LookupKeyLike second = relationship.resolveLookupKey(lookup);

        // Assert
        assertSame(first, second);
    }
}
