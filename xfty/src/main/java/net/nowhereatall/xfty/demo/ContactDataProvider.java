package net.nowhereatall.xfty.demo;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.relationships.DefaultRelationship;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import net.nowhereatall.xfty.values.UniqueEmailExpression;

/** The bundled Contact Provider - {@code accountId} is a required relationship to a generated {@link Account}. */
public final class ContactDataProvider extends SimpleRecordProvider {

    public static final String DEFAULT_FIRST_NAME_PREFIX = "Contact First Name";
    public static final String DEFAULT_LAST_NAME_PREFIX = "Contact Last Name";
    public static final String DEFAULT_EMAIL_PREFIX = "test.contact";

    public ContactDataProvider() {
        super(new MasterTemplate(Field.of(Contact.class, "id"))
                .put(Field.of(Contact.class, "email"), new UniqueEmailExpression(DEFAULT_EMAIL_PREFIX))
                .put(Field.of(Contact.class, "firstName"), new IncrementingStringExpression(DEFAULT_FIRST_NAME_PREFIX))
                .put(Field.of(Contact.class, "lastName"), new IncrementingStringExpression(DEFAULT_LAST_NAME_PREFIX))
                .putRequired(Field.of(Contact.class, "accountId"), new DefaultRelationship(new Account())));
    }
}
