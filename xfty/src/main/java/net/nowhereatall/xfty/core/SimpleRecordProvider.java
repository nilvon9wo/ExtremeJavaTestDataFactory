package net.nowhereatall.xfty.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.engine.RecordFactory;

/**
 * Base for a Provider that is nothing more than a Master Template - by far the
 * common case. Implements the {@link RecordProviderLike} plumbing so a Provider
 * definition is just its template:
 *
 * <pre>{@code
 * final class ContactUnderAccountProvider extends SimpleRecordProvider {
 *     ContactUnderAccountProvider() {
 *         super(new MasterTemplate(Field.of(Contact.class, "id"))
 *             .putRequired(Field.of(Contact.class, "accountId"), new DefaultRelationship(new Account())));
 *     }
 * }
 * }</pre>
 *
 * Implement {@link RecordProviderLike} directly when {@link #createBundle} needs
 * behaviour beyond "build the template".
 */
public abstract class SimpleRecordProvider implements RecordProviderLike {

    private final MasterTemplate masterTemplate;

    protected SimpleRecordProvider(MasterTemplate template) {
        this.masterTemplate = template;
    }

    @Override
    public MasterTemplate masterTemplate() {
        return this.masterTemplate;
    }

    @Override
    public Field primaryTargetField() {
        return this.masterTemplate.primaryTargetField();
    }

    @Override
    public CompletableFuture<Bundle> createBundle(GenerationContext context, List<Object> templateRecords) {
        return RecordFactory.createBundle(context, this.masterTemplate, templateRecords);
    }
}
