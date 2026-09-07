# Porting log — C# XFTY → Java

A running record of decisions made porting XFTY from
[ExtremeCSharpTestDataFactory](https://github.com/nilvon9wo/ExtremeCSharpTestDataFactory)
to Java. History, not current-state docs. The C# source is the primary
reference (kept read-only under `/reference/`, gitignored); its own
`docs/contribute/porting-history.md` is the Apex→C# log.

Guiding principle, carried over from the C# port: **port class-for-class,
method-for-method.** Do not redesign, generalize, or delete a source class in
favour of a "simpler" Java equivalent without an explicit decision. The
deviations below are the ones the commissioning prompt (`/prompt.md`) called
for, plus the unavoidable language-shape differences.

---

## 2026-09-07 — scaffold + first checkpoint (field tokens, lookup, mutation dispatch)

### Build / project shape

- **Gradle (Kotlin DSL) multi-module, JUnit 5**, per the prompt. Modules:
  `xfty` (core) and `xfty-jpa` (persistence binding, stubbed this commit).
  Gradle is not installed on the dev box — the wrapper was bootstrapped from a
  downloaded 8.10.2 distribution and is committed.
- **Java 17 toolchain.** Records have been stable since 16; `-parameters` is on.
- Package root **`net.nowhereatall.xfty`** (prompt decision 6), artifacts
  **`xfty`** / **`xfty-jpa`**. Maven Central groupId is **`io.github.nilvon9wo`**
  (changed 2026-09-07): the `net.nowhereatall` namespace would need a DNS TXT
  record proving control of `nowhereatall.net`, and Brian was not sure he
  controls that domain or how to add the record; `io.github.<user>` is verified
  automatically against the GitHub account with no DNS. Central verifies the
  groupId, not the package, so the package name is unaffected. Publishing +
  signing skeleton is in `build.gradle.kts`; the Maven Central OIDC Trusted
  Publishing workflow is not wired yet (next).
- Test conventions mirror the C# port: one test class per unit, one behaviour
  per method, **literal** `// Arrange` / `// Act` / `// Assert` /
  `// Sanity Check` comments, names adapted to Java (`methodDoesX`).

### Naming: interfaces get an adjective, implementations get the noun

Brian's rule (stated 2026-09-07, overriding the first draft below): **an
interface — a role/contract — is named with an adjective; the clean noun is
left for the implementation.** `-able`/`-ible` where it reads naturally,
otherwise a `-Like` suffix. Explicitly *against* Clean Code's advice to hide
interface-ness and give the impl a name like `…Impl` — Brian rejects `Impl`.

So every ported C# `IFoo` becomes Java `FooLike`, and the plain `Foo` is free
for the concrete type (which, for the collision cases, is exactly the C#
concrete name):

| C# interface | Java interface | C# concrete → Java concrete |
|----|----|----|
| `ILookupKey` | `LookupKeyLike` | `LookupKey` → `LookupKey` (the type-only key) |
| `IProviderLookup` | `ProviderLookupLike` | — (`MapBackedLookup`) |
| `IRecordProvider` | `RecordProviderLike` | `RecordProvider` → `RecordProvider` (later; minimal now) |
| `IRecordPredicate` | `RecordPredicateLike` | — (`FieldEqualToPredicate`, …) |
| `ISharedAncestorDefaults` | `SharedAncestorDefaultsLike` | — |
| `IPersistenceGateway` | `PersistenceGatewayLike` | `EfPersistenceGateway` → `JpaPersistenceGateway` |
| `IUnsetFieldFiller` | `UnsetFieldFillerLike` | (add-on modules, later) |

`ProviderLookups` (the C# static helper class, plural) keeps its name — it is
not an interface. `FlavouredLookupKey`/`DiscriminatorLookupKey` are unchanged
(already qualified concrete names).

### `Field` — the field-token layer (prompt decision 1)

C# `Field.Of<T>(x => x.Name)` returns a raw `PropertyInfo`; every map in the
library keys by that. Java has no single "property" reflective type, so
`net.nowhereatall.xfty.Field` is a small immutable token that *is* the key
(equality by `(recordType, name)`), resolves + holds the reader, and exposes
`get(record)`.

- **Both overloads, as specified:**
  - `Field.of(Account.class, "name")` — string, universal fallback. Resolution
    order: record component → bean getter (`getName` / `isActive`) → declared
    field (any hierarchy level, `setAccessible`). Unknown name → named
    `XftyConfigurationException` quoting the type and field.
  - `Field.of(Account::getName)` — takes `SerializableFunction<T,R>`
    (`extends Function, Serializable`, defined in this package). Decomposed via
    `writeReplace` → `SerializedLambda`. Recovered `implMethodName` is mapped
    to a logical field name handling **both** conventions: bean `getX`/`isX` →
    decapitalised `X`, and bare record accessor `name()` → `name` (checked
    against `getRecordComponents()`). Then it converges on the same
    `resolve(type, name)` the string overload uses.
- **Failure is loud and named:** `FieldReferenceException extends
  XftyConfigurationException`, thrown for a synthetic lambda body
  (`implMethodName` starts with `lambda$`), a non-instance reference
  (constructor/static), or a non-zero-arg accessor. Every message points at
  `Field.of(Type.class, "name")` as the fallback.
- The owner type comes from `SerializedLambda.getImplClass()`, so
  `Field.of(Account::getName)` needs no explicit `Account.class` argument —
  strictly better ergonomics than the string form, matching the C# lambda
  overload.

### `RecordShape` — record-vs-bean mutation dispatch (prompt decision 2)

`net.nowhereatall.xfty.reflect.RecordShape` is the **single dispatch point**
choosing per `Class.isRecord()`. Everything else calls `with` / `copy` /
`blank` and never branches on record-ness.

- **Classic mutable class:** `with` mutates in place and returns the *same*
  instance. Write path (`BeanFieldWriter`): public `setX(...)` if present,
  else the backing field with `setAccessible(true)` — the same init-only /
  private-setter bypass the C# port gets from `PropertyInfo.SetValue`. `copy`
  = no-arg construct + copy every non-static declared field. `blank` = no-arg
  constructor (required; named error otherwise).
- **Java record:** `with` reads every component via its accessor into the
  canonical-constructor argument array (component order == constructor param
  order, guaranteed by the spec), overrides the changed one(s), and
  reconstructs — a *new* instance. `copy` = reconstruct with current values.
  `blank` = canonical constructor with per-type defaults.
- Multi-field `with(Map<Field,Object>)` does one reconstruction for a record,
  not one per field. Null values survive the map via an internal sentinel.

This subsumes the three C# scatter points (`IdMocker`, `RecordCloneFactory`,
`RecordInjector` all reflectively `SetValue` after construction) into one
strategy switch, as the prompt requires.

### Lookup / variant / specificity (prompt decision 3) — ported ~1:1

`LookupKey` (flyweight per `Class`), `FlavouredLookupKey` (flyweight per
type+flavour, predicates not part of identity, `specificity = 20 + predicate
count`), `DiscriminatorLookupKey` (named convenience over a single
`equalTo` predicate), `ProviderLookupLike`, `ProviderLookups` (`resolve` /
`bestOf` / `reconcile` / `keysFor` / `of` / `ofTypes`), `MapBackedLookup`.
Java's `Class` identity via `getClass()` gives the same exact-type-equality
`GetType()` does, so the specificity tie-break is unchanged. Ambiguous
equally-specific matches → `LookupException` naming the fix, same as C#.

- `MapBackedLookup.registerSharedAncestorDefaults()` throws
  `UnsupportedOperationException` until the shared-ancestor subsystem is
  ported — mirrors the identical early stub in the C# port's history.

### Predicates — ported (self-contained, "the easy win" per the C# log)

`RecordPredicate` + `FieldEqualToPredicate`, `FieldInSetPredicate`,
`FieldGreaterThan/LessThanPredicate`, `NegationPredicate`, `AllOf`/`AnyOf`,
`PredicateFactory`, `FieldPredicateFactory` (with `Type::accessor` overloads),
`ValueComparison`. `ValueComparison` numeric path uses `BigDecimal` (C#
`decimal`); same-typed `Comparable`s (dates/times) compare naturally;
everything else lexicographically.

### `RecordProvider` — minimal this pass

Reduced to `Class<?> primaryType()` — all the lookup system needs to route.
The full surface (master template, primary target field, bundle creation)
lands with the generation engine. Flagged so it is not mistaken for the
finished shape.

### Tests

49 tests, all green (`./gradlew :xfty:test`): `FieldTest` (10),
`RecordShapeTest` (9), `LookupKeyTest` (5), `FlavouredLookupKeyTest` (6),
`DiscriminatorLookupKeyTest` (3), `ProviderLookupsTest` (8),
`FieldPredicatesTest` (8).

---

## 2026-09-07 — value expressions + Maven Central publishing

### `values/` — the self-contained (non-context) value expressions

`ValueExpressionLike` (C# `IValueExpression`) + `LiteralExpression`,
`IncrementingDecimalExpression` (C# `decimal` → `BigDecimal`),
`IncrementingStringExpression` (`SEPARATE_PREFIX`/`DONT_SEPARATE_PREFIX`
constants kept), `UniqueStringExpression`, `UniqueEmailExpression`,
`UniqueStringOfLengthExpression` (base-26, recursive, no loop),
`UniqueAcrossRunsExpression` (run token = 9 digits of epoch-ms + 5 of
randomness). `ContextAwareExpressionLike` / `DeferredExpressionLike` and their
`CopyFrom*` implementations are deferred until `GenerationContext` /
`DeferredGraph` exist, exactly as the C# port deferred them.

- **Deviation, flagged:** the process-static counters use `AtomicInteger`
  instead of C#'s plain `static int` + `++`. The C# port keeps a plain field
  and disables xUnit parallelism to match Apex's single-threaded semantics;
  for these leaf value classes `AtomicInteger` is a zero-behaviour-change
  idiomatic improvement, so it is used here rather than relying on a
  test-runner setting. Tests assert *relative* behaviour (counter advances,
  values differ), never absolute counter values, since the counter is
  JVM-wide across the whole test run.
- 10 tests (`ValueExpressionsTest`). 59 total.

### Maven Central — coordinates, bundle, workflows

- Group id `io.github.nilvon9wo` (verified against the GitHub account, no DNS).
  Package stays `net.nowhereatall.xfty`; artifacts `xfty` / `xfty-jpa`.
- **`com.gradleup.nmcp.settings` 1.6.2** bundles both modules into one Central
  Portal deployment (`nmcpPublishAggregationToCentralPortal`). POM carries
  name/description/url/license/developers/scm; `withSourcesJar()` +
  `withJavadocJar()`; in-memory PGP signing that only engages when
  `SIGNING_KEY` is set (so local builds don't need a key).
- **`.github/workflows/ci.yml`** — `./gradlew build` on push/PR to `main`
  (Temurin 17, `gradle/actions/setup-gradle`).
- **`.github/workflows/publish.yml`** — on `v*` tag: build, then bundle +
  upload as a *draft* deployment (`publishingType = "USER_MANAGED"`), version
  derived from the tag.
- **OIDC:** Maven Central has **no trusted-publishing equivalent** to the C#
  side's NuGet OIDC flow (as of 2026-09 — npm/PyPI/NuGet do, Central went the
  Sigstore route instead). The Portal upload authenticates with a revocable
  user token (two repo secrets). `publish.yml` already declares
  `id-token: write` so the eventual move to keyless Sigstore signing (dropping
  the PGP secret) is config-only. Full reasoning + the secrets to set:
  `docs/publishing.md`.

---

## 2026-09-07 — the generation engine (vertical slice: `supply()` works)

`new RecordProvider(Contact.class, lookup).setInsertMode(MOCK)
.setInclusivity(REQUIRED).supply()` now produces a `Contact` with defaults, a
generated `Account` ancestor, and the FK wired between them - end to end,
records and beans alike.

**Ported:** `InsertMode`/`InsertInclusivity`, `MasterTemplate` (partials
merged), `PathValue`/`PathTargetValue`/`PathTargetValueKind`,
`AncestorPathWalker`, `InverseAlignment`, `Bundle` (parents + primaries;
children/enrichment/deferred-queue omitted), `GenerationContext`,
`AncestorCycleGuard`, `ValueFieldPass`, `RecordProviderLike` (full),
`SimpleRecordProvider`, `RecordProvider` + `RecordProviderTemplateConfig` +
`RecordProviderConflictException`, engine passes (`PlainValueFiller`,
`ContextAwareValuePass`, `RelationshipForcer`, `PathValueApplier`,
`LookupWiring`, `AncestorGenerator`, `RecordFactory`), `PersistenceGatewayLike`
+ `IdMocker`, `UnsetFieldFillerLike`, `ContextAwareExpressionLike` +
`CopyFromSiblingExpression` + `CopyFromAncestorExpression`, and the demo
`AccountDataProvider`/`ContactDataProvider`/`DefaultProviderLookup`.

**Two structural adaptations, both because a Java record is immutable:**

1. **Synchronous, not `async`.** The C# port is `Task`-based end to end
   ("every real backing store already works that way"). Java's persistence
   world - JPA, JDBC - is synchronous; `CompletableFuture` everywhere would be
   noise. `PersistenceGatewayLike.insert` returns `void`, `RecordFactory` /
   `RecordProvider.supply*` are plain calls. The C# port added async in a later
   pass, so this matches its earlier shape too.
2. **Passes thread the record through instead of mutating in place.** C#'s
   `PlainValueFiller` / `LookupWiring` / `ContextAwareValuePass` / `IdMocker`
   all do `field.SetValue(record, x)` and rely on later steps seeing it. For a
   Java record, `RecordShape.with(record, field, x)` returns a *new* instance,
   so every pass takes the current record(s), applies its changes, and writes
   the result back to `bundle.putPrimaries(...)`. Within
   `ContextAwareValuePass.completeRow` the per-field context
   (`GenerationContext.recordBeingBuilt`) is re-pointed at the rebuilt instance
   before each next field, so a sibling read always sees current state. One
   dispatch point (`RecordShape`), no record/bean branching in the passes
   themselves.

**Deferred to later passes** (stubbed or simply absent, as the C# port
staged them): children (`ChildProvider`, `RecordProvider.with*`),
`SharedAncestor`/`SharedRelationship` (`PathTargetValue.isSharedRelationship()`
returns false), deferred/up-flow values + depth-batched insert
(`DeferredExpressionLike`, `CopyFromDescendantExpression`, `DeferredGraph`,
`DepthBatchedInserter`, ...), `InsertMode.NOW` real persistence (throws without
a gateway, as in C#), the typed `MasterTemplate<T>` / `RecordProvider<T>`
wrappers, and enrichment.

**Tests:** `RecordProviderIntegrationTest` (11) - defaults, override-wins,
mock ids, `NEVER` leaves id unset, required-relationship FK wiring, `NONE`
skips it, quantity → N distinct, context-aware sibling (success + the loud
throw on a still-pending sibling), context-aware ancestor copy. 74 total,
green.

### Still to port

Children, shared ancestors, deferred/depth-batched persistence, `InsertMode.NOW`
against a real gateway, the typed wrappers, enrichment, `xfty-jpa`, and keyless
signing.
