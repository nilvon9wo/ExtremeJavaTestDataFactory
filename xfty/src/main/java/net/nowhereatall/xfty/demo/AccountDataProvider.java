package net.nowhereatall.xfty.demo;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.SimpleRecordProvider;
import net.nowhereatall.xfty.values.IncrementingStringExpression;
import net.nowhereatall.xfty.values.LiteralExpression;

/** The bundled Account Provider - a starter-kit example of a declarative Master Template. */
public final class AccountDataProvider extends SimpleRecordProvider {

    public static final String DEFAULT_NAME_PREFIX = "Test Account Name";
    public static final String DEFAULT_INDUSTRY = "Test Account Industry";
    public static final String DEFAULT_TYPE = "Test Account Type";

    public AccountDataProvider() {
        super(new MasterTemplate(Field.of(Account.class, "id"))
                .put(Field.of(Account.class, "name"), new IncrementingStringExpression(DEFAULT_NAME_PREFIX))
                .put(Field.of(Account.class, "industry"), new LiteralExpression(DEFAULT_INDUSTRY))
                .put(Field.of(Account.class, "type"), new LiteralExpression(DEFAULT_TYPE)));
    }
}
