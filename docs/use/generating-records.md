# Generating records

## One record

```java
Contact result = Async.await(new RecordProvider<>(Contact.class, lookup)
    .supply());

// result.id() == null  - not inserted by default
```

## Several records

```java
List<Contact> contacts = Async.await(new RecordProvider<>(Contact.class, lookup)
    .setQuantityPerTemplate(5)
    .supplyList());
```

## Shorthand constructors

| Form | Type comes from |
|------|-----------------|
| `new RecordProvider<>(Contact.class, lookup)` | the class |
| `new RecordProvider<>(Contact.builder()...build(), lookup)` | the template |
| `new RecordProvider<>(List.of(a, b), lookup)` | the first template |
| `new RecordProvider<Contact>(LookupKey.get(Contact.class), lookup)` | the key |

```java
Contact fromTemplate = Async.await(
    new RecordProvider<>(Contact.builder().firstName("Alice").build(), lookup).supply());

List<Contact> fromList = Async.await(
    new RecordProvider<>(List.of(Contact.builder().build(), Contact.builder().build()), lookup).supplyList());

Contact fromKey = Async.await(
    new RecordProvider<Contact>(LookupKey.get(Contact.class), lookup).supply());
```

## Insert modes

| Mode | Effect |
|------|--------|
| `NEVER` (default) | in-memory only; no id |
| `MOCK` | a placeholder `"mock-N"` id, no database |
| `NOW` | a real insert through the configured `PersistenceGateway` |
| `DEFERRED` | register the graph; `DeferredInserter.flush(gateway)` saves it later, in dependency order |

`.depthBatched()` (with `NOW`) inserts one batch per dependency layer instead of
one per Provider.

---

Examples verified by `ExGeneratingRecordsTest`.
