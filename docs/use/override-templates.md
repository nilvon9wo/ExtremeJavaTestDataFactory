# Override templates

An **override template** is a partly-filled record you hand to a
`RecordProvider`. XFTY keeps the values you set and generates the rest.

## The simplest case

```java
Contact result = Async.await(new RecordProvider<>(Contact.class, lookup)
    .setOverrideTemplate(Contact.builder().firstName("Alice").lastName("Smith").build())
    .supply());

// result.firstName() == "Alice", result.lastName() == "Smith"
// result.email() is still generated
```

Or the shorthand constructor, which takes the type from the template:

```java
Contact shorthand = Async.await(
    new RecordProvider<>(Contact.builder().firstName("Alice").build(), lookup).supply());
```

## Precedence

An override template beats everything - a Provider default, a `put(...)`, a
context-aware value:

```java
Contact result = Async.await(new RecordProvider<>(Contact.class, lookup)
    .put(Contact::firstName, new LiteralExpression("Generated"))
    .setOverrideTemplate(Contact.builder().firstName("Alice").build())
    .supply());

// result.firstName() == "Alice"  (not "Generated")
```

## Removing values

To make XFTY leave a field alone entirely, remove it from the template:

```java
Contact result = Async.await(new RecordProvider<>(Contact.class, lookup)
    .removeFromMasterTemplate(Contact::email)
    .supply());

// result.email() == null
```

---

Examples verified by `ExOverrideTemplatesTest`.
