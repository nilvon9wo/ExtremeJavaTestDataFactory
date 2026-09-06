package net.nowhereatall.xfty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.nowhereatall.xfty.demo.Account;
import net.nowhereatall.xfty.demo.Contact;
import org.junit.jupiter.api.Test;

class FieldTest {

    @Test
    void resolvesBeanGetterFromAStringName() {
        // Arrange
        String fieldName = "name";

        // Act
        Field field = Field.of(Account.class, fieldName);

        // Assert
        assertEquals("name", field.name());
        assertEquals(Account.class, field.recordType());
        assertEquals(String.class, field.valueType());
    }

    @Test
    void resolvesBeanGetterFromAMethodReference() {
        // Arrange
        SerializableFunction<Account, String> reference = Account::getName;

        // Act
        Field field = Field.of(reference);

        // Assert
        assertEquals("name", field.name());
        assertEquals(Account.class, field.recordType());
    }

    @Test
    void mapsAnIsPrefixedBooleanAccessorToTheBareFieldName() {
        // Arrange
        SerializableFunction<Account, Boolean> reference = Account::isActive;

        // Act
        Field field = Field.of(reference);

        // Assert
        assertEquals("active", field.name());
        assertEquals(boolean.class, field.valueType());
    }

    @Test
    void mapsARecordAccessorToItsComponentName() {
        // Arrange
        SerializableFunction<Contact, String> reference = Contact::firstName;

        // Act
        Field field = Field.of(reference);

        // Assert
        assertEquals("firstName", field.name());
        assertEquals(Contact.class, field.recordType());
    }

    @Test
    void resolvesARecordComponentFromAStringName() {
        // Arrange
        String componentName = "accountId";

        // Act
        Field field = Field.of(Contact.class, componentName);

        // Assert
        assertEquals("accountId", field.name());
        assertEquals(String.class, field.valueType());
    }

    @Test
    void readsTheCurrentValueOffARecord() {
        // Arrange
        Contact contact = new Contact("c1", "Ada", "Lovelace", "ada@example.com", "a1");
        Field field = Field.of(Contact.class, "firstName");

        // Act
        Object value = field.get(contact);

        // Assert
        assertEquals("Ada", value);
    }

    @Test
    void twoTokensForTheSameFieldAreEqualAndHashAlike() {
        // Arrange
        Field fromString = Field.of(Account.class, "industry");
        Field fromReference = Field.of((SerializableFunction<Account, String>) Account::getIndustry);

        // Act
        boolean equal = fromString.equals(fromReference);

        // Assert
        assertTrue(equal);
        assertEquals(fromString.hashCode(), fromReference.hashCode());
    }

    @Test
    void tokensForDifferentFieldsAreNotEqual() {
        // Arrange
        Field name = Field.of(Account.class, "name");
        Field industry = Field.of(Account.class, "industry");

        // Act / Assert
        assertNotEquals(name, industry);
    }

    @Test
    void rejectsAnUnknownFieldNameWithANamedException() {
        // Arrange
        String missing = "notAField";

        // Act
        XftyConfigurationException thrown = assertThrows(
                XftyConfigurationException.class,
                () -> Field.of(Account.class, missing));

        // Assert
        assertTrue(thrown.getMessage().contains("notAField"));
    }

    @Test
    void rejectsALambdaBodyAndPointsAtTheStringOverload() {
        // Arrange
        SerializableFunction<Account, String> notAMethodReference =
                account -> account.getName().toUpperCase();

        // Act
        FieldReferenceException thrown = assertThrows(
                FieldReferenceException.class,
                () -> Field.of(notAMethodReference));

        // Assert
        assertTrue(thrown.getMessage().contains("Field.of(Type.class"));
    }
}
