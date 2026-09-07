package net.nowhereatall.xfty.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.CopyFromSiblingExpression;
import net.nowhereatall.xfty.values.LiteralExpression;
import org.junit.jupiter.api.Test;

/** Proves {@link PathTargetValue} - the value half of a {@link PathValue}. {@code applyTo} lands on a master template. */
class PathTargetValueTest {

    @Test
    void ofLiteral_IsNotARelationshipAndAppliesTheLiteral() {
        // Arrange
        PathTargetValue value = PathTargetValue.ofLiteral("Acme");
        MasterTemplate template = new MasterTemplate(Field.of(Account.class, "id"));

        // Act
        value.applyTo(template, Field.of(Account.class, "name"));

        // Assert
        assertFalse(value.isRelationship());
        assertTrue(template.defaultByField().containsKey(Field.of(Account.class, "name")));
    }

    @Test
    void ofExpression_AppliesTheExpression() {
        // Arrange
        PathTargetValue value = PathTargetValue.ofExpression(new LiteralExpression("X"));
        MasterTemplate template = new MasterTemplate(Field.of(Account.class, "id"));

        // Act
        value.applyTo(template, Field.of(Account.class, "name"));

        // Assert
        assertTrue(template.defaultByField().containsKey(Field.of(Account.class, "name")));
    }

    @Test
    void ofRequiredRelationship_IsARelationship() {
        // Arrange
        PathTargetValue value = PathTargetValue.ofRequiredRelationship(new DefaultRelationship(new Account()));

        // Act / Assert
        assertTrue(value.isRelationship());
        assertFalse(value.isSharedRelationship());
    }

    @Test
    void ofOptionalRelationship_AppliesAsAnOptionalRelationship() {
        // Arrange
        PathTargetValue value = PathTargetValue.ofOptionalRelationship(new DefaultRelationship(new Account()));
        MasterTemplate template = new MasterTemplate(Field.of(Contact.class, "id"));

        // Act
        value.applyTo(template, Field.of(Contact.class, "accountId"));

        // Assert
        assertTrue(template.optionalRelationshipByField().containsKey(Field.of(Contact.class, "accountId")));
    }

    @Test
    void ofContextAware_AppliesAsAContextAwareExpression() {
        // Arrange
        PathTargetValue value = PathTargetValue.ofContextAware(CopyFromSiblingExpression.from(Account::getName));
        MasterTemplate template = new MasterTemplate(Field.of(Account.class, "id"));

        // Act
        value.applyTo(template, Field.of(Account.class, "site"));

        // Assert
        assertTrue(template.contextAwareByField().containsKey(Field.of(Account.class, "site")));
    }
}
