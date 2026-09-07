package net.nowhereatall.xfty.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;

/**
 * The definitions that generate one record type (and its related graph). The
 * seam the lookup/variant system stores and returns.
 *
 * <p>(C# {@code IRecordProvider}. The plain noun {@code RecordProvider} is the
 * concrete fluent entry-point class.)
 */
public interface RecordProviderLike {

    /** The primary key / identity field of the generated record. */
    Field primaryTargetField();

    /** This provider's Master Template. */
    MasterTemplate masterTemplate();

    /** Turn this provider's Master Template plus {@code templateRecords} into a wired {@link Bundle}. */
    CompletableFuture<Bundle> createBundle(GenerationContext context, List<Object> templateRecords);
}
