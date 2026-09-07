package net.nowhereatall.xfty.persistence;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.nowhereatall.xfty.Field;

/**
 * The one seam a real backing store plugs into. {@code InsertMode.NOW} throws
 * everywhere in this library unless a gateway is supplied
 * ({@code RecordProvider.setPersistenceGateway}).
 *
 * <p>Deliberately the smallest possible surface: persist a batch of records of
 * one type, and write each one's generated identifier back onto {@code idField}
 * - the same shape {@link IdMocker} uses for {@code InsertMode.MOCK}, so a real
 * gateway is a drop-in swap. Nothing here mentions a storage technology; an
 * implementation is free to use JPA, JDBC, an HTTP client, a vector-database
 * client, or an in-memory fake.
 *
 * <p>Asynchronous end to end, like the C# {@code IPersistenceGateway} it ports:
 * every real backing store - a database driver, a network client - is
 * ultimately non-blocking, so generation is {@link CompletableFuture}-based
 * throughout rather than assuming one particular (e.g. JPA/JDBC) synchronous
 * model.
 */
public interface PersistenceGatewayLike {

    /**
     * Persist every record in {@code records} - all the same type - and give each
     * its real, generated identifier on {@code idField}. Returns the persisted
     * records <b>in the same order</b>: a mutable-class gateway (JPA, JDBC)
     * returns the same instances it was given; a record-friendly gateway returns
     * fresh instances carrying the id, since a Java record cannot be mutated.
     */
    CompletableFuture<List<Object>> insert(List<Object> records, Field idField);
}
