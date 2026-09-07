package net.nowhereatall.xfty.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.reflect.RecordShape;

/**
 * Assigns a placeholder identifier to records before (or instead of) a real
 * insert - {@code InsertMode.MOCK}'s whole job, and useful for pure in-memory
 * unit tests generally, since they never get a real identity-column round-trip.
 * The generated value is a simple unique string; nothing downstream parses its
 * format.
 *
 * <p>A record is immutable, so setting its id yields a new instance - callers
 * must use the returned list.
 */
public final class IdMocker {

    private static final AtomicInteger FAKE_COUNT = new AtomicInteger(0);

    private IdMocker() {
    }

    public static List<Object> addIds(List<Object> records, Field idField) {
        List<Object> withIds = new ArrayList<>(records.size());
        for (Object record : records) {
            withIds.add(addId(record, idField));
        }
        return withIds;
    }

    public static Object addId(Object record, Field idField) {
        return RecordShape.of(record.getClass()).set(record, idField, generateId());
    }

    public static String generateId() {
        return "mock-" + FAKE_COUNT.incrementAndGet();
    }
}
