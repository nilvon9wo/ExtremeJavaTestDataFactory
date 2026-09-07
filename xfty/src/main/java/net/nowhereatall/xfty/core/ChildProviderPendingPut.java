package net.nowhereatall.xfty.core;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.relationships.DefaultRelationshipLike;
import net.nowhereatall.xfty.values.ContextAwareExpressionLike;
import net.nowhereatall.xfty.values.ValueExpressionLike;

/** One field configuration queued on a {@link ChildProvider}, applied to the real {@link RecordProvider} once it exists. */
public final class ChildProviderPendingPut {

    private final Field field;
    private final ChildProviderPendingPutKind kind;
    private final Object payload;

    private ChildProviderPendingPut(Field field, ChildProviderPendingPutKind kind, Object payload) {
        this.field = field;
        this.kind = kind;
        this.payload = payload;
    }

    public static ChildProviderPendingPut ofValue(Field field, ValueExpressionLike expression) {
        return new ChildProviderPendingPut(field, ChildProviderPendingPutKind.VALUE, expression);
    }

    public static ChildProviderPendingPut ofContextAware(Field field, ContextAwareExpressionLike expression) {
        return new ChildProviderPendingPut(field, ChildProviderPendingPutKind.CONTEXT_AWARE, expression);
    }

    public static ChildProviderPendingPut ofRequiredRelationship(Field field, DefaultRelationshipLike relationship) {
        return new ChildProviderPendingPut(field, ChildProviderPendingPutKind.REQUIRED_RELATIONSHIP, relationship);
    }

    public static ChildProviderPendingPut ofOptionalRelationship(Field field, DefaultRelationshipLike relationship) {
        return new ChildProviderPendingPut(field, ChildProviderPendingPutKind.OPTIONAL_RELATIONSHIP, relationship);
    }

    public static ChildProviderPendingPut ofLiteral(Field field, Object literal) {
        return new ChildProviderPendingPut(field, ChildProviderPendingPutKind.LITERAL, literal);
    }

    public void applyTo(RecordProvider<?> provider) {
        switch (this.kind) {
            case VALUE -> provider.put(this.field, (ValueExpressionLike) this.payload);
            case CONTEXT_AWARE -> provider.put(this.field, (ContextAwareExpressionLike) this.payload);
            case REQUIRED_RELATIONSHIP -> provider.putRequired(this.field, (DefaultRelationshipLike) this.payload);
            case OPTIONAL_RELATIONSHIP -> provider.putOptional(this.field, (DefaultRelationshipLike) this.payload);
            case LITERAL -> provider.put(this.field, this.payload);
        }
    }
}
