# Per-call relationship control

`setInclusivity(...)` sets the default; these override it for one call, without
touching the Provider.

| Method | Effect |
|--------|--------|
| `includeOptional(field)` | generate this optional relationship on this call |
| `excludeRelationship(field)` | do **not** generate this one, even if required |
| `excludeRelationshipIfPresent(field)` | as above, a no-op if the field isn't a relationship |
| `includeOptional(List.of(a, b, ...))` | reach down a path - force every hop |

## The simplest case

```java
Account result = Async.await(new RecordProvider<>(Account.class, lookup)
    .includeOptional(Account::getOwnerId)         // generate this optional one too
    .excludeRelationship(Account::getParentId)    // do not generate this one
    .setInsertMode(InsertMode.MOCK)
    .supply());

// result.getOwnerId() != null, result.getParentId() == null
```

## Reaching deeper - a path

A path forces every relationship along it, even at the default `NONE`
inclusivity:

```java
Bundle bundle = Async.await(new RecordProvider<>(Contact.class, lookup)
    .includeOptional(List.of(Field.of(Contact.class, "accountId"), Field.of(Account.class, "ownerId")))
    .setInclusivity(InsertInclusivity.REQUIRED)
    .supplyBundle());

Bundle accountBundle = bundle.getBundle(Contact.class, "accountId");
// accountBundle.getList(Field.of(Account.class, "ownerId")) is populated
```

`put(List.of(...path..., targetField), value)` does the same walk and also sets
a value on the record it reaches - see the path-value examples.

---

Examples verified by `ExPerCallRelationshipsTest`.
