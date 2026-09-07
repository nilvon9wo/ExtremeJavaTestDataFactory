package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.CopyFromSiblingExpression;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import net.nowhereatall.xfty.values.LiteralExpression;
import net.nowhereatall.xfty.values.ValueExpressionLike;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Proves {@link MasterTemplate} - the declarative description of how one record type is generated. Pure in-memory. */
class MasterTemplateTest {

    private static final Field ACCOUNT_ID = Field.of(Account.class, "id");
    private static final Field ACCOUNT_NAME = Field.of(Account.class, "name");
    private static final Field CONTACT_ID = Field.of(Contact.class, "id");
    private static final Field CONTACT_LAST_NAME = Field.of(Contact.class, "lastName");
    private static final Field CONTACT_ACCOUNT_ID = Field.of(Contact.class, "accountId");
    private static final Field CONTACT_REPORTS_TO_ID = Field.of(Contact.class, "reportsToId");

    @Test
    void constructor_SeedsEmptyMapsAndKeepsThePrimaryTargetField() {
        // Act
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID);

        // Assert
        assertEquals(ACCOUNT_ID, template.primaryTargetField());
        assertTrue(template.defaultByField().isEmpty());
        assertTrue(template.requiredRelationshipByField().isEmpty());
        assertTrue(template.optionalRelationshipByField().isEmpty());
    }

    @Test
    void put_ForAPlainValueExpression_RoutesItToTheDefaultMap() {
        // Act
        MasterTemplate template = new MasterTemplate(CONTACT_ID).put(CONTACT_LAST_NAME, new LiteralExpression("Doe"));

        // Assert
        assertTrue(template.defaultByField().containsKey(CONTACT_LAST_NAME));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void put_ForAContextAwareExpression_RoutesItToTheContextAwareMap(boolean passAsObject) {
        // Arrange
        ContextAwareExpressionLike contextAware = CopyFromSiblingExpression.from(Contact::firstName);
        MasterTemplate template = new MasterTemplate(CONTACT_ID);

        // Act
        if (passAsObject) {
            template.put(CONTACT_LAST_NAME, (Object) contextAware);
        } else {
            template.put(CONTACT_LAST_NAME, contextAware);
        }

        // Assert
        assertEquals(contextAware, template.contextAwareByField().get(CONTACT_LAST_NAME));
        assertFalse(template.defaultByField().containsKey(CONTACT_LAST_NAME));
        assertTrue(template.orderedValueFields().contains(CONTACT_LAST_NAME));
    }

    @Test
    void put_WhenReplacingAPlainFieldWithAContextAwareExpression_MovesItBetweenMaps() {
        // Act
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID)
                .put(ACCOUNT_NAME, new LiteralExpression("plain"))
                .put(ACCOUNT_NAME, CopyFromSiblingExpression.from(Account::getAccountNumber));

        // Assert
        assertFalse(template.defaultByField().containsKey(ACCOUNT_NAME));
        assertTrue(template.contextAwareByField().containsKey(ACCOUNT_NAME));
    }

    @Test
    void put_ForABareLiteral_WrapsItAsAnExactValue() {
        // Act
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID).put(Field.of(Account.class, "type"), "Customer");

        // Assert
        assertTrue(template.defaultByField().containsKey(Field.of(Account.class, "type")));
        assertEquals("Customer", template.defaultByField().get(Field.of(Account.class, "type")).get());
    }

    @Test
    void put_ForAnExistingExpressionPassedAsObject_DoesNotDoubleWrapIt() {
        // Arrange
        ValueExpressionLike expression = new IncrementingStringExpression("Acct");

        // Act
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID).put(ACCOUNT_NAME, (Object) expression);

        // Assert
        assertSame(expression, template.defaultByField().get(ACCOUNT_NAME));
    }

    @Test
    void put_WhenGivenARelationshipAsObject_Throws() {
        // Act
        XftyConfigurationException thrown = assertThrows(XftyConfigurationException.class,
                () -> new MasterTemplate(CONTACT_ID).put(CONTACT_ACCOUNT_ID, (Object) new DefaultRelationship(new Account())));

        // Assert
        assertTrue(thrown.getMessage().contains("putRequired"));
    }

    @Test
    void putRequired_RoutesTheRelationshipToTheRequiredMap() {
        // Arrange
        DefaultRelationship required = new DefaultRelationship(new Account());

        // Act
        MasterTemplate template = new MasterTemplate(CONTACT_ID).putRequired(CONTACT_ACCOUNT_ID, required);

        // Assert
        assertSame(required, template.requiredRelationshipByField().get(CONTACT_ACCOUNT_ID));
        assertFalse(template.defaultByField().containsKey(CONTACT_ACCOUNT_ID));
    }

    @Test
    void putOptional_RoutesTheRelationshipToTheOptionalMap() {
        // Arrange
        DefaultRelationship optional = new DefaultRelationship(Contact.builder().build());

        // Act
        MasterTemplate template = new MasterTemplate(CONTACT_ID).putOptional(CONTACT_REPORTS_TO_ID, optional);

        // Assert
        assertSame(optional, template.optionalRelationshipByField().get(CONTACT_REPORTS_TO_ID));
    }

    @Test
    void remove_ClearsTheFieldFromEveryMapAndFromTheOrderedFields() {
        // Arrange
        MasterTemplate template = new MasterTemplate(CONTACT_ID)
                .put(CONTACT_LAST_NAME, new LiteralExpression("Doe"))
                .putRequired(CONTACT_ACCOUNT_ID, new DefaultRelationship(new Account()));

        // Act
        template.remove(CONTACT_LAST_NAME);
        template.remove(CONTACT_ACCOUNT_ID);

        // Assert
        assertFalse(template.defaultByField().containsKey(CONTACT_LAST_NAME));
        assertFalse(template.requiredRelationshipByField().containsKey(CONTACT_ACCOUNT_ID));
        assertFalse(template.orderedValueFields().contains(CONTACT_LAST_NAME));
    }

    @Test
    void orderedValueFields_FollowsPutOrder() {
        // Arrange
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID)
                .put(ACCOUNT_NAME, new LiteralExpression("n"))
                .put(Field.of(Account.class, "industry"), new LiteralExpression("i"))
                .put(Field.of(Account.class, "type"), new LiteralExpression("t"));

        // Act / Assert
        assertEquals(
                List.of(ACCOUNT_NAME, Field.of(Account.class, "industry"), Field.of(Account.class, "type")),
                template.orderedValueFields());
    }

    @Test
    void orderedValueFields_AfterRemoveThenRePut_KeepsTheFieldInItsOriginalPlace() {
        // Arrange
        MasterTemplate template = new MasterTemplate(ACCOUNT_ID)
                .put(ACCOUNT_NAME, new LiteralExpression("n"))
                .put(Field.of(Account.class, "industry"), new LiteralExpression("i"))
                .put(Field.of(Account.class, "type"), new LiteralExpression("t"));
        template.remove(Field.of(Account.class, "industry"));

        // Act
        template.put(ACCOUNT_NAME, new LiteralExpression("n2"));

        // Assert
        assertEquals(List.of(ACCOUNT_NAME, Field.of(Account.class, "type")), template.orderedValueFields());
    }

    @Test
    void copy_ReflectsItsOwnEditsAndLeavesTheOriginalUntouched() {
        // Arrange
        MasterTemplate original = new MasterTemplate(ACCOUNT_ID).put(ACCOUNT_NAME, new LiteralExpression("Original"));

        // Act
        MasterTemplate copy = original.copy();
        copy.put(Field.of(Account.class, "industry"), new LiteralExpression("Tech"));
        copy.remove(ACCOUNT_NAME);

        // Assert
        assertTrue(copy.defaultByField().containsKey(Field.of(Account.class, "industry")));
        assertFalse(copy.defaultByField().containsKey(ACCOUNT_NAME));
        assertEquals(ACCOUNT_ID, copy.primaryTargetField());
        assertFalse(original.defaultByField().containsKey(Field.of(Account.class, "industry")));
        assertTrue(original.defaultByField().containsKey(ACCOUNT_NAME));
    }

    @Test
    void copy_CarriesAContextAwareEntry() {
        // Arrange
        MasterTemplate template =
                new MasterTemplate(ACCOUNT_ID).put(ACCOUNT_NAME, CopyFromSiblingExpression.from(Account::getAccountNumber));

        // Act / Assert
        assertTrue(template.copy().contextAwareByField().containsKey(ACCOUNT_NAME));
    }
}
