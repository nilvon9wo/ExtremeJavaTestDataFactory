package net.nowhereatall.xfty.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * The one place that chooses, per {@link Class#isRecord()}, how to write a
 * field after construction:
 *
 * <ul>
 *   <li><b>Classic mutable class</b> - direct mutation via a setter, or the
 *       backing field with {@code setAccessible(true)} when there is none (the
 *       same bypass a private/final-ish setter would need). The instance handed
 *       in is the instance handed back.</li>
 *   <li><b>Java record</b> - every current component value is read via its
 *       accessor, the one that changed is overridden, and a fresh instance is
 *       built through the canonical constructor. A <em>new</em> instance comes
 *       back; records are immutable.</li>
 * </ul>
 *
 * Every other part of the engine goes through {@link #with} / {@link #copy} /
 * {@link #blank} and never branches on record-ness itself.
 */
public final class RecordShape {

    private static final Map<Class<?>, RecordShape> CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = Map.of(
            boolean.class, Boolean.FALSE,
            byte.class, (byte) 0,
            short.class, (short) 0,
            int.class, 0,
            long.class, 0L,
            float.class, 0f,
            double.class, 0d,
            char.class, '\0');

    private final Class<?> type;
    private final boolean record;
    private final List<Field> fields;
    private final Constructor<?> canonicalConstructor;
    private final List<RecordComponent> components;

    private RecordShape(Class<?> type) {
        this.type = type;
        this.record = type.isRecord();
        this.components = this.record ? List.of(type.getRecordComponents()) : List.of();
        this.canonicalConstructor = this.record ? canonicalConstructorOf(type) : null;
        this.fields = resolveFields(type);
    }

    public static RecordShape of(Class<?> type) {
        if (type == null) {
            throw new XftyConfigurationException("A record shape requires a type.");
        }
        return CACHE.computeIfAbsent(type, RecordShape::new);
    }

    public boolean isRecord() {
        return this.record;
    }

    /** Every logical field (record component or bean getter), in declaration order. */
    public List<Field> fields() {
        return this.fields;
    }

    public Object get(Object instance, Field field) {
        return field.get(instance);
    }

    /** Return {@code instance} with {@code field} set to {@code value} - mutated in place for a class, rebuilt for a record. */
    public Object with(Object instance, Field field, Object value) {
        return with(instance, Map.of(field, wrapNull(value)));
    }

    /** As {@link #with(Object, Field, Object)} for several fields at once. */
    public Object with(Object instance, Map<Field, Object> changes) {
        requireInstance(instance);
        return this.record
                ? rebuiltRecord(instance, changes)
                : mutatedInPlace(instance, changes);
    }

    /** A full-fidelity copy: a new instance carrying every current value. */
    public Object copy(Object instance) {
        requireInstance(instance);
        return this.record
                ? rebuiltRecord(instance, Map.of())
                : copiedClass(instance);
    }

    /** {@code quantity} independent copies of {@code instance}. */
    public List<Object> copies(Object instance, int quantity) {
        List<Object> copies = new ArrayList<>(quantity);
        for (int index = 0; index < quantity; index++) {
            copies.add(copy(instance));
        }
        return copies;
    }

    /** A new instance with every field at its type default (null / 0 / false). */
    public Object blank() {
        if (this.record) {
            Object[] defaults = this.components.stream()
                    .map(component -> defaultValue(component.getType()))
                    .toArray();
            return construct(this.canonicalConstructor, defaults);
        }
        return newClassInstance();
    }

    // Record path ------------------------------------------------------------

    private Object rebuiltRecord(Object instance, Map<Field, Object> changes) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        changes.forEach((field, value) -> overrides.put(field.name(), unwrapNull(value)));
        Object[] arguments = this.components.stream()
                .map(component -> overrides.containsKey(component.getName())
                        ? overrides.get(component.getName())
                        : accessorValue(component, instance))
                .toArray();
        return construct(this.canonicalConstructor, arguments);
    }

    private static Object accessorValue(RecordComponent component, Object instance) {
        try {
            component.getAccessor().setAccessible(true);
            return component.getAccessor().invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new XftyConfigurationException("Could not read record component '" + component.getName() + "'.", e);
        }
    }

    private static Constructor<?> canonicalConstructorOf(Class<?> type) {
        Class<?>[] parameterTypes = new Class<?>[type.getRecordComponents().length];
        for (int index = 0; index < parameterTypes.length; index++) {
            parameterTypes[index] = type.getRecordComponents()[index].getType();
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new XftyConfigurationException(
                    type.getSimpleName() + " has no canonical constructor - it cannot be reconstructed.", e);
        }
    }

    // Class path -----------------------------------------------------------

    private Object mutatedInPlace(Object instance, Map<Field, Object> changes) {
        changes.forEach((field, value) -> BeanFieldWriter.write(instance, field, unwrapNull(value)));
        return instance;
    }

    private Object copiedClass(Object instance) {
        Object copy = newClassInstance();
        for (java.lang.reflect.Field stateField : instanceStateFields(this.type)) {
            try {
                stateField.setAccessible(true);
                stateField.set(copy, stateField.get(instance));
            } catch (IllegalAccessException e) {
                throw new XftyConfigurationException(
                        "Could not copy field '" + stateField.getName() + "' of " + this.type.getSimpleName() + ".", e);
            }
        }
        return copy;
    }

    private Object newClassInstance() {
        try {
            Constructor<?> constructor = this.type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new XftyConfigurationException(
                    this.type.getSimpleName() + " needs a no-argument constructor for XFTY to build or copy it.", e);
        } catch (ReflectiveOperationException e) {
            throw new XftyConfigurationException("Could not instantiate " + this.type.getSimpleName() + ".", e);
        }
    }

    private static List<java.lang.reflect.Field> instanceStateFields(Class<?> type) {
        List<java.lang.reflect.Field> stateFields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    stateFields.add(field);
                }
            }
        }
        return stateFields;
    }

    // Shared -------------------------------------------------------------

    private static Object construct(Constructor<?> constructor, Object[] arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException e) {
            throw new XftyConfigurationException(
                    "Could not reconstruct " + constructor.getDeclaringClass().getSimpleName() + " via its canonical constructor.", e);
        }
    }

    private static List<Field> resolveFields(Class<?> type) {
        List<Field> resolved = new ArrayList<>();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                resolved.add(Field.of(type, component.getName()));
            }
            return resolved;
        }
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                continue;
            }
            String name = beanPropertyName(method.getName());
            if (name != null && method.getDeclaringClass() != Object.class) {
                Field field = Field.of(type, name);
                if (!resolved.contains(field)) {
                    resolved.add(field);
                }
            }
        }
        return resolved;
    }

    private static String beanPropertyName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return java.beans.Introspector.decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return java.beans.Introspector.decapitalize(methodName.substring(2));
        }
        return null;
    }

    private static Object defaultValue(Class<?> type) {
        return type.isPrimitive() ? PRIMITIVE_DEFAULTS.get(type) : null;
    }

    private void requireInstance(Object instance) {
        if (instance == null) {
            throw new XftyConfigurationException("A " + this.type.getSimpleName() + " instance is required, not null.");
        }
        if (!this.type.isInstance(instance)) {
            throw new XftyConfigurationException(
                    "Expected a " + this.type.getSimpleName() + " but got " + instance.getClass().getSimpleName() + ".");
        }
    }

    private static final Object NULL_SENTINEL = new Object();

    private static Object wrapNull(Object value) {
        return value == null ? NULL_SENTINEL : value;
    }

    private static Object unwrapNull(Object value) {
        return value == NULL_SENTINEL ? null : value;
    }
}
