package net.nowhereatall.xfty.jpa.demo;

import java.util.Map;
import java.util.Set;

import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.lookup.LookupKey;
import net.nowhereatall.xfty.lookup.LookupKeyLike;
import net.nowhereatall.xfty.lookup.ProviderLookupLike;
import net.nowhereatall.xfty.lookup.ProviderLookups;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import net.nowhereatall.xfty.values.LiteralExpression;
import net.nowhereatall.xfty.values.UniqueEmailExpression;

/** The bundled providers + lookup for the {@code xfty-jpa} demo entities. */
public final class JpaDemoProviders {

    private JpaDemoProviders() {
    }

    public static ProviderLookupLike lookup() {
        return new JpaDemoLookup();
    }

    public static final class JpaAccountProvider extends SimpleRecordProvider {
        public JpaAccountProvider() {
            super(MasterTemplate.of(JpaAccount::getId)
                    .put(JpaAccount::getName, new IncrementingStringExpression("Test Account Name"))
                    .put(JpaAccount::getIndustry, new LiteralExpression("Test Industry"))
                    .put(JpaAccount::getType, new LiteralExpression("Test Type")));
        }
    }

    public static final class JpaContactProvider extends SimpleRecordProvider {
        public JpaContactProvider() {
            super(MasterTemplate.of(JpaContact::getId)
                    .put(JpaContact::getEmail, new UniqueEmailExpression("test.contact"))
                    .put(JpaContact::getFirstName, new IncrementingStringExpression("Contact First Name"))
                    .put(JpaContact::getLastName, new IncrementingStringExpression("Contact Last Name"))
                    .putRequired(JpaContact::getAccountId, new DefaultRelationship(new JpaAccount())));
        }
    }

    private static final class JpaDemoLookup implements ProviderLookupLike {
        private final Map<LookupKeyLike, RecordProviderLike> byKey = Map.of(
                LookupKey.get(JpaAccount.class), new JpaAccountProvider(),
                LookupKey.get(JpaContact.class), new JpaContactProvider());

        @Override
        public RecordProviderLike get(Class<?> recordType) {
            return get(LookupKey.get(recordType));
        }

        @Override
        public RecordProviderLike get(LookupKeyLike lookupKey) {
            return ProviderLookups.get(this.byKey, lookupKey);
        }

        @Override
        public Set<LookupKeyLike> keysFor(Object record) {
            return ProviderLookups.keysFor(Set.copyOf(this.byKey.keySet()), record);
        }
    }
}
