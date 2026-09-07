package net.nowhereatall.xfty.enrichment;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.XftyConfigurationException;

/**
 * Turns the hops of an injection path into the navigation property the enricher
 * grafts onto. A plain foreign-key-shaped field carries no relationship-name
 * metadata, so this resolves one by convention: a lookup field named {@code Xid}
 * grafts the ancestor onto a sibling {@code X} property on the same record; a
 * child collection grafts onto whichever property on the parent type holds a
 * {@code List} of the child's own type.
 */
public final class InjectionPathResolver {

    private static final String ID_SUFFIX = "Id";

    private InjectionPathResolver() {
    }

    /** The ancestor navigation field for a lookup field - {@code Contact.account} for {@code Contact.accountId}. */
    public static Field parentRelationshipField(Field lookupField) {
        String name = lookupField.name();
        if (!name.endsWith(ID_SUFFIX) || name.length() == ID_SUFFIX.length()) {
            throw badHop(describe(lookupField) + " does not follow the <name>Id lookup convention - it cannot be an ancestor hop.");
        }
        String relationshipName = name.substring(0, name.length() - ID_SUFFIX.length());
        Class<?> declaringType = lookupField.recordType();
        try {
            return Field.of(declaringType, relationshipName);
        } catch (RuntimeException absent) {
            throw badHop(declaringType.getSimpleName() + " has no " + relationshipName + " property to graft an ancestor under.");
        }
    }

    /** The child-collection navigation field on {@code parentType} matching {@code childLookupField}'s own record type. */
    public static Field childRelationshipField(Class<?> parentType, Field childLookupField) {
        Class<?> childType = childLookupField.recordType();
        List<Method> candidates = new ArrayList<>();
        for (Method method : parentType.getMethods()) {
            if (method.getParameterCount() == 0 && List.class.isAssignableFrom(method.getReturnType())
                    && elementType(method) == childType) {
                candidates.add(method);
            }
        }
        if (candidates.size() == 1) {
            return Field.of(parentType, beanPropertyName(candidates.get(0).getName()));
        }
        if (candidates.isEmpty()) {
            throw badHop(parentType.getSimpleName() + " has no collection property of " + childType.getSimpleName()
                    + " - it cannot be an injected subquery for " + describe(childLookupField) + ".");
        }
        throw badHop(parentType.getSimpleName() + " has " + candidates.size() + " collection properties of "
                + childType.getSimpleName() + " - injection cannot tell which one " + describe(childLookupField) + " means.");
    }

    private static Class<?> elementType(Method listGetter) {
        Type generic = listGetter.getGenericReturnType();
        if (generic instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }
        return null;
    }

    private static String beanPropertyName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return java.beans.Introspector.decapitalize(methodName.substring(3));
        }
        return methodName;
    }

    private static String describe(Field field) {
        return field.recordType().getSimpleName() + "." + field.name();
    }

    private static XftyConfigurationException badHop(String detail) {
        return new XftyConfigurationException("Injection path: " + detail);
    }
}
