package net.nowhereatall.xfty.relationships;

import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.GenerationContext;

/**
 * A relationship whose parent record is <b>shared</b> - every child that
 * references it gets the same record (and the same id), generated at most once
 * per test method. The factory branches on this interface: instead of one parent
 * per child it resolves the shared record once and points every child at it.
 *
 * <p>(C# {@code ISharedRelationship}.)
 */
public interface SharedRelationshipLike extends DefaultRelationshipLike {

    /** The name this shared record is interned under. */
    String sharedName();

    /** The one shared record - generated (and cached) on first call, reused after. */
    CompletableFuture<Object> resolveSharedRecord(GenerationContext context);

    /** A single-record sub-bundle exposing the shared record. Never null once the record is resolved. */
    Bundle getResolvedBundle();

    /** Whether the shared record has a real (inserted) id - a NOW child needs this to be true. */
    boolean isResolvedRecordPersisted();

    /** Whether the shared record has been generated yet. */
    boolean isResolved();
}
