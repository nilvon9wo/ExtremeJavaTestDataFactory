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
| Generation engine (context-aware values, relationships, bundles) | ⬜ not started |
| Enrichment | ⬜ not started |
| `xfty-jpa` persistence binding (H2 + Testcontainers Postgres) | ⬜ scaffolded only |
| Maven Central publishing (OIDC Trusted Publishing) | ⬜ skeleton only |

Java records are a first-class target, fully co-equal with classic mutable
classes — every place the engine would set a field after construction, a record
is instead reconstructed through its canonical constructor.

## Build

```bash
./gradlew build      # compile + test everything
./gradlew :xfty:test # core module tests only
```

Java 17+. The Gradle wrapper is committed; nothing else to install.

## License

MIT — see [LICENSE](LICENSE).
