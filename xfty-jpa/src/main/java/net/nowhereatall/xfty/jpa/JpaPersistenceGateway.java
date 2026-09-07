package net.nowhereatall.xfty.jpa;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.persistence.EntityManager;
import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.persistence.PersistenceGatewayLike;
import net.nowhereatall.xfty.reflect.RecordShape;

/**
 * The real, database-backed {@link PersistenceGatewayLike} - the piece that
 * makes {@code InsertMode.NOW} actually persist, proven against a real JPA
 * {@link EntityManager} rather than a mock. Register it with
 * {@code recordProvider.setPersistenceGateway(new JpaPersistenceGateway(entityManager))}.
 *
 * <p>A String-typed id with no value is filled with a fresh UUID before
 * {@code persist} - the common shape for a String primary key, which JPA has no
 * built-in generator for (unlike an {@code @GeneratedValue} numeric id, which
 * the provider populates on {@code flush} on its own, left untouched here).
 *
 * <p>This is the direct analog of the C# port's {@code EfPersistenceGateway}.
 * The persistence machinery is synchronous under the hood (JPA is), so the
 * returned future is already complete - the async signature is there so a
 * different backing store (a reactive driver, an HTTP client) can be genuinely
 * non-blocking.
 *
 * <p>JPA entities cannot be Java records (the spec requires a mutable,
 * no-arg-constructible class), so gateway targets are always classic classes and
 * are updated in place.
 */
public final class JpaPersistenceGateway implements PersistenceGatewayLike {

    private final EntityManager entityManager;

    public JpaPersistenceGateway(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CompletableFuture<List<Object>> insert(List<Object> records, Field idField) {
        for (Object record : records) {
            fillStringIdIfAbsent(record, idField);
            this.entityManager.persist(record);
        }
        this.entityManager.flush();
        return CompletableFuture.completedFuture(records);
    }

    private static void fillStringIdIfAbsent(Object record, Field idField) {
        if (idField.valueType() == String.class && idField.get(record) == null) {
            RecordShape.of(record.getClass()).set(record, idField, UUID.randomUUID().toString());
        }
    }
}
