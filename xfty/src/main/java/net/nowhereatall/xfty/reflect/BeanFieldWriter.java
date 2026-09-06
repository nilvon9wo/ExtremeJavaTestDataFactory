package net.nowhereatall.xfty.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * Writes one field on a classic mutable class: through a public
 * {@code setX(...)} setter when one exists, otherwise straight to the backing
 * field with {@code setAccessible(true)} - the same bypass an
 * init-only/private setter would otherwise block, matching how the C# port
 * relies on {@code PropertyInfo.SetValue} ignoring init-only.
 */
final class BeanFieldWriter {

    private BeanFieldWriter() {
    }

    static void write(Object instance, Field field, Object value) {
        Method setter = setterFor(field);
        if (setter != null) {
            invokeSetter(instance, setter, value, field);
            return;
        }
        writeBackingField(instance, field, value);
    }

    private static Method setterFor(Field field) {
        String setterName = "set" + Character.toUpperCase(field.name().charAt(0)) + field.name().substring(1);
        for (Method method : field.recordType().getMethods()) {
            if (method.getName().equals(setterName)
                    && method.getParameterCount() == 1
                    && box(method.getParameterTypes()[0]).isAssignableFrom(box(field.valueType()))) {
                return method;
            }
        }
        return null;
    }

    private static void invokeSetter(Object instance, Method setter, Object value, Field field) {
        try {
            setter.setAccessible(true);
            setter.invoke(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new XftyConfigurationException("Could not set " + field + " via " + setter.getName() + "(...).", e);
        }
    }

    private static void writeBackingField(Object instance, Field field, Object value) {
        java.lang.reflect.Field backingField = field.backingField() != null
                ? field.backingField()
                : declaredField(field);
        try {
            backingField.setAccessible(true);
            if (Modifier.isFinal(backingField.getModifiers())) {
                throw new XftyConfigurationException(
                        "Cannot set final field " + field + " on a classic class - give it a setter or make it non-final. "
                        + "(Records are handled separately, by reconstruction.)");
            }
            backingField.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new XftyConfigurationException("Could not set " + field + " by field access.", e);
        }
    }

    private static java.lang.reflect.Field declaredField(Field field) {
        for (Class<?> current = field.recordType(); current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(field.name());
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new XftyConfigurationException(
                field.recordType().getSimpleName() + " has no setter and no field named '" + field.name() + "' to write.");
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return Character.class;
    }
}
