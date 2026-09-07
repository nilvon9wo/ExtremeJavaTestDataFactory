package net.nowhereatall.xfty.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.demo.Contact;
import net.nowhereatall.xfty.demo.DefaultProviderLookup;
import net.nowhereatall.xfty.values.LiteralExpression;
import org.junit.jupiter.api.Test;

/** Runs the exact code shown in {@code docs/use/override-templates.md}. */
class ExOverrideTemplatesTest {

    private static final DefaultProviderLookup LOOKUP = new DefaultProviderLookup();

    @Test
    void theSimplestCase() {
        // from docs/use/override-templates.md "The simplest case"
        Contact result = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .setOverrideTemplate(Contact.builder().firstName("Alice").lastName("Smith").build())
                .supply());

        assertEquals("Alice", result.firstName());
        assertEquals("Smith", result.lastName());
        assertNotNull(result.email()); // still generated

        // the shorthand constructor form
        Contact shorthand = Async.await(
                new RecordProvider<>(Contact.builder().firstName("Alice").build(), LOOKUP).supply());
        assertEquals("Alice", shorthand.firstName());
    }

    @Test
    void precedence_TheOverrideTemplateWins() {
        // from docs/use/override-templates.md "Precedence"
        Contact result = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .put(Contact::firstName, new LiteralExpression("Generated"))
                .setOverrideTemplate(Contact.builder().firstName("Alice").build())
                .supply());

        assertEquals("Alice", result.firstName()); // not "Generated"
    }

    @Test
    void removingValues() {
        // from docs/use/override-templates.md "Removing values"
        Contact result = Async.await(new RecordProvider<>(Contact.class, LOOKUP)
                .removeFromMasterTemplate(Contact::email)
                .supply());

        assertNull(result.email());
    }
}
