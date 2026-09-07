# XFTY — Extreme Java Test Data Factory

A declarative test data factory for Java. Describe only the values a test
actually cares about; XFTY supplies sensible defaults, creates related records,
and either mocks persistence or inserts for real through a pluggable
`PersistenceGateway`.

This is a Java port of
[ExtremeCSharpTestDataFactory](https://github.com/nilvon9wo/ExtremeCSharpTestDataFactory)
(itself ported from Apex). See [`docs/porting-log.md`](docs/porting-log.md) for
the running port decisions.

## Status: early port in progress

| Area | State |
|------|-------|
| Field tokens — `Field.of(Account.class, "name")` and `Field.of(Account::getName)` | ✅ built + tested |
| Record ⇄ bean mutation dispatch (`RecordShape`) | ✅ built + tested |
| Lookup / variant / specificity system | ✅ built + tested |
| Predicates | ✅ built + tested |
| Value expressions (literal, counters, unique tokens) | ✅ built + tested |
| Generation engine — `supply()` with defaults, overrides, required/optional relationships, context-aware sibling & ancestor values | ✅ built + tested |
| Downward child collections | ✅ built + tested |
| `InsertMode.NOW` real persistence + `xfty-jpa` (H2 tier + Testcontainers Postgres tier) | ✅ built + tested |
| Deferred / depth-batched insert (`InsertMode.DEFERRED`, `.depthBatched()`, up-flow values) | ✅ built + tested |
| Shared ancestors | ⬜ not started |
| Enrichment (`bundle.inject(...)` / `injectAll` - relationship + child-collection + scalar injection via reflection) | ✅ built + tested |
| Maven Central publishing | 🔶 wired (`io.github.nilvon9wo:xfty`); see [docs/publishing.md](docs/publishing.md) |

Java records are a first-class target, fully co-equal with classic mutable
classes — every place the engine would set a field after construction, a record
is instead reconstructed through its canonical constructor.

## Example

```java
ProviderLookupLike lookup = new DefaultProviderLookup();

// A Contact with sensible defaults and a generated Account, foreign key wired:
Bundle bundle = new RecordProvider(Contact.class, lookup)
    .setInsertMode(InsertMode.MOCK)
    .setInclusivity(InsertInclusivity.REQUIRED)
    .supplyBundle();

Contact contact = (Contact) bundle.primaryRecords().get(0);
Account account = (Account) bundle.getList(Field.of(Contact.class, "accountId")).get(0);
assertEquals(account.getId(), contact.accountId());

// Override only what the test cares about:
Account acme = (Account) new RecordProvider(Account.class, lookup)
    .put(Account::getName, "Acme")
    .supply();
```

## Build

```bash
./gradlew build      # compile + test everything
./gradlew :xfty:test # core module tests only
```

Java 17+. The Gradle wrapper is committed; nothing else to install.

## License

MIT — see [LICENSE](LICENSE).
