package net.nowhereatall.xfty.engine;

import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;
import net.nowhereatall.xfty.core.MasterTemplate;
import net.nowhereatall.xfty.core.UnsetFieldFillerLike;
import net.nowhereatall.xfty.persistence.IdMocker;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;
import net.nowhereatall.xfty.reflect.RecordShape;

/** Turns one master template plus the test's own partial templates into a wired {@link Bundle}. */
public final class RecordFactory {

    private final GenerationContext context;
    private final MasterTemplate template;

    private RecordFactory(GenerationContext context, MasterTemplate masterTemplate) {
        this.context = context;
        MasterTemplate forced = RelationshipForcer.apply(context.forcedRelationshipPaths(), masterTemplate);
        this.template = PathValueApplier.apply(context.pathValues(), forced);
    }

    public static Bundle createBundle(GenerationContext context, MasterTemplate masterTemplate, List<Object> testTemplates) {
        return new RecordFactory(context, masterTemplate).build(testTemplates);
    }

    private Bundle build(List<Object> testTemplates) {
        int quantity = testTemplates.size();
        Bundle bundle = new AncestorGenerator(this.context, quantity, this.template).generate();
        List<Object> records = PlainValueFiller.cloneAndCompletePlainValues(this.template, testTemplates);
        bundle.putPrimaries(this.template.primaryTargetField(), records);
        new LookupWiring(bundle, this.context, this.template).wire();
        new ContextAwareValuePass(bundle, this.context, this.template).complete();
        fillUnsetFields(bundle);
        persist(bundle);
        return bundle;
    }

    /**
     * Runs after every value/relationship pass, before persistence - late enough
     * that a filler never fights XFTY for a field XFTY actually set, early enough
     * that a real {@code InsertMode.NOW} database still sees a value for a NOT
     * NULL column XFTY never cared about.
     */
    private void fillUnsetFields(Bundle bundle) {
        UnsetFieldFillerLike filler = this.context.unsetFieldFiller();
        if (filler == null) {
            return;
        }
        RecordShape shape = RecordShape.of(this.template.primaryTargetField().recordType());
        List<Field> unsetFields = new ArrayList<>();
        for (Field field : shape.fields()) {
            if (!this.template.isConfigured(field)) {
                unsetFields.add(field);
            }
        }
        if (unsetFields.isEmpty()) {
            return;
        }
        List<Object> records = bundle.primaryRecords();
        List<Object> filled = new ArrayList<>(records.size());
        for (Object record : records) {
            filled.add(filler.fill(record, unsetFields));
        }
        bundle.putPrimaries(this.template.primaryTargetField(), filled);
    }

    private void persist(Bundle bundle) {
        if (this.context.excludePrimaryIds()) {
            return;
        }
        List<Object> records = bundle.primaryRecords();
        Field primaryTargetField = this.template.primaryTargetField();
        switch (this.context.insertMode()) {
            case MOCK -> bundle.putPrimaries(primaryTargetField, IdMocker.addIds(records, primaryTargetField));
            case NOW -> insertNow(records, primaryTargetField);
            default -> {
                // NEVER / LATER / DEFERRED - nothing to do here yet.
            }
        }
    }

    private void insertNow(List<Object> records, Field primaryTargetField) {
        PersistenceGatewayLike gateway = this.context.persistenceGateway();
        if (gateway == null) {
            throw new UnsupportedOperationException(
                    "InsertMode.NOW needs a persistence gateway - RecordProvider.setPersistenceGateway(...) - use "
                    + "MOCK or NEVER when none is configured.");
        }
        gateway.insert(records, primaryTargetField);
    }
}
