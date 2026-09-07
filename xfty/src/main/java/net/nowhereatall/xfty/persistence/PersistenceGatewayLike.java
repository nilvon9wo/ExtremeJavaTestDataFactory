package net.nowhereatall.xfty.persistence;

import java.util.List;

import net.nowhereatall.xfty.Field;

/**
 * The one seam a real backing store plugs into. {@code InsertMode.NOW} throws
 * everywhere in this library unless a gateway is supplied
 * ({@code RecordProvider.setPersistenceGateway}).
 *
 * <p>Deliberately the smallest possible surface: persist a batch of records of
 * one type, and write each one's generated identifier back onto
 * {@code idField} - the same shape {@link IdMocker} uses for
 * {@code InsertMode.MOCK}, so a real gateway is a drop-in swap. Nothing here
 * mentions a storage technology.
 *
 * <p>(C# {@code IPersistenceGateway}. Synchronous here - the Java persistence
 * world, JPA included, is synchronous; see docs/porting-log.md.)
 */
public interface PersistenceGatewayLike {

    /**
     * Persist every record in {@code records} - all the same type - and set
     * {@code idField} on each to its real, generated identifier. Gateway targets
     * are mutable classes, so the records are updated in place.
     */
    void insert(List<Object> records, Field idField);
}
