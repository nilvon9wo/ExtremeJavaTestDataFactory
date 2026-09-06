package net.nowhereatall.xfty;

import java.beans.Introspector;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves one logical field on a record type to the token every other type in
 * this library keys its maps by.
 *
 * <p>Two ways to name a field:
 * <ul>
 *   <li>{@code Field.of(Account.class, "name")} - string-based, always works,
 *       any class, zero requirements. The universal fallback.</li>
 *   <li>{@code Field.of(Account::getName)} - preferred when usable. The
 *       {@link SerializableFunction} method reference is decomposed via
 *       {@link SerializedLambda} to recover the accessor name, which is then
 *       mapped to a field. Both JavaBean getters ({@code getName}/{@code isActive})
 *       and Java record accessors (bare {@code name()}) are understood. A
 *       reference that is not a single plain accessor call throws
 *       {@link FieldReferenceException}, pointing at the string overload.</li>
 * </ul>
 *
 * <p>A {@code Field} identifies itself by {@code (recordType, name)} - two
 * tokens for the same field on the same type are equal and hash alike, so they
 * work directly as map keys. Reading is done here ({@link #get}); writing is
 * dispatched by {@link net.nowhereatall.xfty.reflect.RecordShape}, which chooses
 * mutation-in-place or canonical-constructor reconstruction per
 * {@link Class#isRecord()}.
 */
public final class Field {

    private final Class<?> recordType;
    private final String name;
    private final Method accessor;
    private final java.lang.reflect.Field backingField;
    private final Class<?> valueType;

    private Field(Class<?> recordType, String name, Method accessor, java.lang.reflect.Field backingField, Class<?> valueType) {
        this.recordType = recordType;
        this.name = name;
        this.accessor = accessor;
        this.backingField = backingField;
        this.valueType = valueType;
    }

    /** Resolve {@code name} on {@code recordType} - record component, then bean getter, then a declared field. */
    public static Field of(Class<?> recordType, String name) {
        if (recordType == null) {
            throw new XftyConfigurationException("A field token requires a record type.");
        }
        if (name == null || name.isBlank()) {
            throw new XftyConfigurationException("A field token requires a field name.");
        }
        return resolve(recordType, name);
    }

    /** Resolve the accessor a plain method reference like {@code Account::getName} names. */
    public static <T, R> Field of(SerializableFunction<T, R> methodReference) {
        if (methodReference == null) {
            throw new XftyConfigurationException("A field token requires a method reference.");
        }
        SerializedLambda lambda = serializedLambda(methodReference);
        rejectLambdaBody(lambda);
        rejectNonInstanceReference(lambda);
        Class<?> owner = ownerClass(lambda, methodReference);
        String logicalName = logicalNameOf(owner, lambda.getImplMethodName());
        return resolve(owner, logicalName);
    }

    public Class<?> recordType() {
        return this.recordType;
    }

    public String name() {
        return this.name;
    }

    public Class<?> valueType() {
        return this.valueType;
    }

    /** Read this field's current value off {@code record}. */
    public Object get(Object record) {
        if (record == null) {
            throw new XftyConfigurationException("Cannot read " + this + " off a null record.");
        }
        try {
            if (this.accessor != null) {
                return this.accessor.invoke(record);
            }
            return this.backingField.get(record);
        } catch (IllegalAccessException e) {
            throw new XftyConfigurationException("Cannot read " + this + " - the accessor is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw new XftyConfigurationException("Reading " + this + " threw.", e.getTargetException());
        }
    }

    // Resolved reflective handles, for the mutation-dispatch layer -------------

    /** The zero-argument reader (bean getter or record accessor), or {@code null} if this field is reached by a raw field. */
    public Method accessor() {
        return this.accessor;
    }

    /** The declared field this token reads, or {@code null} if it is reached by an accessor method. */
    public java.lang.reflect.Field backingField() {
        return this.backingField;
    }

    // ------------------------------------------------------------------------

    private static Field resolve(Class<?> recordType, String name) {
        if (recordType.isRecord()) {
            for (RecordComponent component : recordType.getRecordComponents()) {
                if (component.getName().equals(name)) {
                    Method accessor = component.getAccessor();
                    accessor.setAccessible(true);
                    return new Field(recordType, name, accessor, null, accessor.getReturnType());
                }
            }
        }

        Optional<Method> getter = beanGetter(recordType, name);
        if (getter.isPresent()) {
            Method accessor = getter.get();
            accessor.setAccessible(true);
            return new Field(recordType, name, accessor, null, accessor.getReturnType());
        }

        java.lang.reflect.Field declared = declaredField(recordType, name);
        if (declared != null) {
            declared.setAccessible(true);
            return new Field(recordType, name, null, declared, declared.getType());
        }

        throw new XftyConfigurationException(recordType.getSimpleName() + " has no field named '" + name + "'.");
    }

    private static Optional<Method> beanGetter(Class<?> type, String name) {
        String suffix = capitalize(name);
        return firstMethod(type, "get" + suffix)
                .or(() -> firstMethod(type, "is" + suffix))
                .or(() -> firstMethod(type, name));
    }

    private static Optional<Method> firstMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == 0
                    && method.getReturnType() != void.class) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private static java.lang.reflect.Field declaredField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        return null;
    }

    private static String logicalNameOf(Class<?> owner, String implMethodName) {
        if (owner.isRecord()) {
            for (RecordComponent component : owner.getRecordComponents()) {
                if (component.getName().equals(implMethodName)) {
                    return implMethodName;
                }
            }
        }
        if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
            return Introspector.decapitalize(implMethodName.substring(3));
        }
        if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
            return Introspector.decapitalize(implMethodName.substring(2));
        }
        return implMethodName;
    }

    private static SerializedLambda serializedLambda(Object methodReference) {
        try {
            Method writeReplace = methodReference.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replacement = writeReplace.invoke(methodReference);
            if (replacement instanceof SerializedLambda serialized) {
                return serialized;
            }
            throw new FieldReferenceException(
                    "The value passed to Field.of(...) is not a method reference. Pass one like Account::getName, "
                    + "or use Field.of(Type.class, \"name\").");
        } catch (NoSuchMethodException e) {
            throw new FieldReferenceException(
                    "The function passed to Field.of(...) is not a serializable method reference (no writeReplace). "
                    + "Pass a method reference like Account::getName, or use Field.of(Type.class, \"name\").", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new FieldReferenceException(
                    "Could not decompose the method reference passed to Field.of(...). Use Field.of(Type.class, \"name\").", e);
        }
    }

    private static void rejectLambdaBody(SerializedLambda lambda) {
        if (lambda.getImplMethodName().startsWith("lambda$")) {
            throw new FieldReferenceException(
                    "Field.of(...) was given a lambda body, not a plain accessor reference like Account::getName. "
                    + "A lambda that does more than call one accessor cannot be decomposed - use Field.of(Type.class, \"name\").");
        }
    }

    private static void rejectNonInstanceReference(SerializedLambda lambda) {
        int kind = lambda.getImplMethodKind();
        boolean instanceInvoke = kind == MethodHandleInfo.REF_invokeVirtual
                || kind == MethodHandleInfo.REF_invokeInterface
                || kind == MethodHandleInfo.REF_invokeSpecial;
        if (!instanceInvoke) {
            throw new FieldReferenceException(
                    "Field.of(...) needs an instance accessor reference like Account::getName, not a static or "
                    + "constructor reference. Use Field.of(Type.class, \"name\").");
        }
        if (!lambda.getImplMethodSignature().startsWith("()")) {
            throw new FieldReferenceException(
                    "Field.of(...) needs a zero-argument accessor reference like Account::getName. "
                    + "Use Field.of(Type.class, \"name\").");
        }
    }

    private static Class<?> ownerClass(SerializedLambda lambda, Object methodReference) {
        String binaryName = lambda.getImplClass().replace('/', '.');
        ClassLoader loader = methodReference.getClass().getClassLoader();
        try {
            return Class.forName(binaryName, false, loader);
        } catch (ClassNotFoundException first) {
            try {
                return Class.forName(binaryName, false, Field.class.getClassLoader());
            } catch (ClassNotFoundException second) {
                throw new FieldReferenceException(
                        "Could not load the type '" + binaryName + "' the method reference points at. "
                        + "Use Field.of(Type.class, \"name\").", second);
            }
        }
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Field field
                && field.recordType.equals(this.recordType)
                && field.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.recordType, this.name);
    }

    @Override
    public String toString() {
        return this.recordType.getSimpleName() + "." + this.name;
    }
}
