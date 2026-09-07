package net.nowhereatall.xfty.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.core.RecordProviderLike;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import org.junit.jupiter.api.Test;

/** Proves multi-variant Provider resolution end to end: one record type, several Providers, chosen by lookup key. */
class MultiVariantProviderTest {

    private static final LookupKeyLike ENTERPRISE = FlavouredLookupKey.get(Account.class, "enterprise");
    private static final LookupKeyLike SMB = FlavouredLookupKey.get(Account.class, "smb");

    private static ProviderLookupLike newLookup() {
        Map<LookupKeyLike, RecordProviderLike> providers = Map.of(
                LookupKey.get(Account.class), new NamedAccountProvider("SMB"),
                ENTERPRISE, new NamedAccountProvider("Enterprise"),
                SMB, new NamedAccountProvider("SMB"),
                LookupKey.get(Contact.class), new EnterpriseParentedContactProvider(ENTERPRISE));
        return ProviderLookups.of(providers);
    }

    @Test
    void get_ForAnExplicitEnterpriseKey_ReturnsTheEnterpriseProvider() {
        assertGetIndustry(ENTERPRISE, "Enterprise");
    }

    @Test
    void get_ForAnExplicitSmbKey_ReturnsTheSmbProvider() {
        assertGetIndustry(SMB, "SMB");
    }

    @Test
    void get_ForThePlainTypeKey_ReturnsTheDefaultProvider() {
        assertGetIndustry(LookupKey.get(Account.class), "SMB");
    }

    @Test
    void supplyBundle_WhenTheProvidersRelationshipPinsAVariantByKey_GeneratesThatVariant() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, newLookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        // Assert
        assertEquals("Enterprise",
                bundle.getList(Account.class, Contact.class, "accountId").get(0).getIndustry());
    }

    @Test
    void supplyBundle_WhenAPerCallRelationshipPinsADifferentVariant_GeneratesThatVariant() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, newLookup())
                .putRequired(Contact::accountId, new DefaultRelationship(SMB, new Account()))
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        // Assert
        assertEquals("SMB", bundle.getList(Account.class, Contact.class, "accountId").get(0).getIndustry());
    }

    @Test
    void supplyBundle_WhenAPerCallRelationshipCarriesNoKey_GeneratesTheDefaultVariant() {
        // Act
        Bundle bundle = Async.await(new RecordProvider<>(Contact.class, newLookup())
                .putRequired(Contact::accountId, new DefaultRelationship(new Account()))
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.MOCK)
                .supplyBundle());

        // Assert
        assertEquals("SMB", bundle.getList(Account.class, Contact.class, "accountId").get(0).getIndustry());
    }

    private static void assertGetIndustry(LookupKeyLike key, String expectedIndustry) {
        Account result = Async.await(
                new RecordProvider<Account>(key, newLookup()).setInsertMode(InsertMode.MOCK).supply());
        assertEquals(expectedIndustry, result.getIndustry());
    }

    static final class NamedAccountProvider extends SimpleRecordProvider {
        NamedAccountProvider(String industry) {
            super(MasterTemplate.of(Account::getId)
                    .put(Account::getName, new IncrementingStringExpression(industry + " Account"))
                    .put(Account::getIndustry, industry));
        }
    }

    static final class EnterpriseParentedContactProvider extends SimpleRecordProvider {
        EnterpriseParentedContactProvider(LookupKeyLike enterpriseKey) {
            super(MasterTemplate.of(Contact::id)
                    .put(Contact::lastName, new IncrementingStringExpression("Variant Contact"))
                    .putRequired(Contact::accountId, new DefaultRelationship(enterpriseKey, new Account())));
        }
    }
}
