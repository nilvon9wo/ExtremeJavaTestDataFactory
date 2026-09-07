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

## Current state (2026-09-07, end of the big session)

**269 tests, 2 skipped (the Docker Postgres tier when Docker is absent), 0
failures. CI green** (build + tests + the Postgres Testcontainers tier +
`docs/verify-doc-examples.py`).

**Ported and working end to end:**

| Area | Notes |
|---|---|
| `Field` tokens (string + `Type::accessor`) | bean + record naming |
| `RecordShape` | the one record/bean mutation-dispatch point (`set`/`setAll`/`copy`/`instantiate`) |
| Lookup / variant / specificity | `LookupKey`, `FlavouredLookupKey`, `DiscriminatorLookupKey`, `ProviderLookup(s)`, `MapBackedLookup` |
| Predicates | full package |
| Value expressions | `Literal`, `Incrementing*`, `Unique*`, `CopyFromSibling`, `CopyFromAncestor`, `CopyFromDescendant` |
| Generation engine | `MasterTemplate`, `GenerationContext`, `Bundle`, `RecordProvider<T>`, `SimpleRecordProvider`, `RecordFactory`, `AncestorGenerator`, the value/relationship passes, path values |
| Children | `ChildProvider` (+ grandchildren), `RecordProviderChildConfig`, `BundleMerger` |
| `InsertMode.NOW` + `xfty-jpa` | `JpaPersistenceGateway`; H2 tier always runs, Postgres/Testcontainers tier `@Tag("docker")` |
| Deferred / depth-batched | `InsertMode.DEFERRED`, `.depthBatched()`, `DepthBatchedInserter`, `DeferredInsertBuffer` (+ write-backs), `DeferredInserter`, up-flow `DescendantValuePass` |
| Enrichment | `bundle.inject*`, `BundleEnricher` recursive walk, `InjectionPathResolver`, `RecordInjector`, `ForcedValues`, `QueryableShapeValidator` |
| Shared ancestors | `SharedAncestor` (+ `SharedAncestorProvider`, `SharedAncestorResolver`, `SharedRelationshipWiring`), `ISharedAncestorDefaults` |
| Docs | `docs/getting-started.md` + `docs/use/{generating-records,override-templates,child-records,per-call-relationships}.md`, every example a run test under `xfty/.../examples/`, verified by `docs/verify-doc-examples.py` (in CI) |
| Maven Central | `io.github.nilvon9wo:xfty` / `:xfty-jpa`, nmcp aggregation, OIDC-adjacent workflow, GPG key generated + published, 3 repo secrets set by Brian |

**`Xfty.Test` translation — done:** all of Predicates (11), Values plain (7) +
`CopyFromSibling` + `ContextAwareExpression`, Lookup (`LookupKeyTest`,
`DiscriminatorLookupKeyTest`), `InverseAlignment`, `AncestorCycleGuard`,
`AncestorCycleTest`, `IdMocker`, `XftyConfigurationException`,
`DepthBatchedInserterTest`, `PersistenceGatewayTest`, `MasterTemplateTest`,
`UnsetFieldFillerTest`, `BundleMergerTest`, `PathValueTest`,
`PathTargetValueTest`, `DeferredValueQueueTest`, `AncestorPathWalkerTest`, plus
new integration tests (`RecordProviderIntegrationTest`,
`DeferredInsertIntegrationTest`, `EnrichmentIntegrationTest`,
`SharedAncestorIntegrationTest`, H2/Postgres persistence) and `Examples/` tests
(`ExGeneratingRecords`, `ExOverrideTemplates`, `ExChildRecords`,
`ExPerCallRelationships`).

**`Xfty.Test` translation — still TODO** (features all ported & green; this is
pure coverage): `Core/` (`BundleTest`, `GenerationContextTest`,
`ChildProviderTest`, `ChildProviderOfTTest`, `RecordProviderApiTest`,
`RecordProviderScenarioTest`, `RecordProviderOfTTest`), `Engine/RecordFactoryTest`,
`Lookup/` (`MultiVariantProviderTest`, `VariantResolutionTest`),
`Relationships/` (`SharedAncestorTest`, `SharedAncestorHierarchyTest`,
`SharedAncestorResetTest`), `Values/` (`CopyFromAncestorExpressionTest`,
`CopyFromDescendantExpressionTest`), all `Enrichment/` unit tests, remaining
`Examples/`, `Demo/`. Then more `docs/use/` + `docs/extend/` pages.

**Then:** the other `Xfty.*` add-on modules. Most-useful-first is likely the
DataFaker binding (`Xfty.Bogus` equivalent — realistic fake data) or the
JUnit 5 `@ExtendWith` extension (`Xfty.Xunit`'s `IsolatesSharedAncestor`).

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

**One structural adaptation, because a Java record is immutable:**

1. **Passes thread the record through instead of mutating in place.** C#'s
   `PlainValueFiller` / `LookupWiring` / `ContextAwareValuePass` / `IdMocker`
   all do `field.SetValue(record, x)` and rely on later steps seeing it. For a
   Java record, `RecordShape.set(record, field, x)` returns a *new* instance,
   so every pass takes the current record(s), applies its changes, and writes
   the result back to `bundle.putPrimaries(...)`. Within
   `ContextAwareValuePass.completeRow` the per-field context
   (`GenerationContext.recordBeingBuilt`) is re-pointed at the rebuilt instance
   before each next field, so a sibling read always sees current state. One
   dispatch point (`RecordShape`), no record/bean branching in the passes.

**Async, matching current C#.** C# XFTY is `Task`-based end to end; the Java
port is `CompletableFuture`-based - `RecordProviderLike.createBundle`,
`RecordProvider.supply*`, `RecordFactory.createBundle`,
`AncestorGenerator.generate`, `PersistenceGatewayLike.insert` return futures;
`AncestorGenerator`'s recursion chains via `thenCompose`. The synchronous
passes are synchronous in C# too and stay that way. Not assuming a synchronous
(JPA/JDBC) persistence model - JPA is only the first binding; other backing
stores are async. A test helper `Async.await(future)` unwraps
`CompletionException` so tests assert the real exception type.

**Not yet reached** (being ported next - not "deferred", the goal is
feature parity with the current C# version): `SharedAncestor`/`SharedRelationship`,
deferred/up-flow + depth-batched insert, enrichment, and the rest of the
`Xfty.Test` translation.

---

## 2026-09-07 (cont.) — course-correct + children + `xfty-jpa` + async + generics

**Feedback taken** (see `feedback-match-current-csharp` memory): match the
*current* C# version feature-for-feature, no deferring by "C# staged it that
way"; async end-to-end (don't assume JPA/synchronous - JPA is only the first
binding); verb method names; port `Xfty.Test`.

- **Async end to end.** `CompletableFuture` throughout -
  `RecordProviderLike.createBundle`, `RecordProvider.supply*`, `RecordFactory`,
  `AncestorGenerator` (recursion via `thenCompose`), `PersistenceGatewayLike.insert`.
  `net.nowhereatall.xfty.Async.await(future)` unwraps `CompletionException`.
- **`RecordProvider<T>` generic** - `supply()`/`supplyList()` return
  `CompletableFuture<T>`/`<List<T>>`, `put(T::accessor, v)` needs no cast.
  `Bundle.getPrimaries(Class<R>)` / `getList(Class<R>, ownerType, "field")` are
  typed. `MasterTemplate.of(Type::accessor)` names the primary field by ref.
- **`RecordShape.with` → `set`** (methods are verbs); `blank` → `instantiate`.
- **Children ported** - `ChildProvider` (+ `ChildProviderPendingPut`),
  `RecordProviderChildConfig`, `BundleChildEntry`, `BundleMerger`, Bundle's
  child methods; `RecordProvider.with(ChildProvider)`/`withChild`/`withChildren`.
- **`xfty-jpa` + `InsertMode.NOW`.** `JpaPersistenceGateway` via
  `EntityManager.persist` (the analog of C#'s `EfPersistenceGateway`).
  `xfty-jpa` test module: mutable `@Entity` demo classes (`JpaAccount`/
  `JpaContact` - JPA entities cannot be records, exactly the expected
  constraint), Hibernate-bootstrapped **H2 tier that always runs** (3 tests:
  real insert, required-parent FK wiring, quantity) and a **Testcontainers
  Postgres tier tagged `docker`** that `assumeTrue`-skips without Docker.
- **Demo domain** fleshed out to the full C# field set + nav slots, with
  builders on `Account`/`Contact`/`Case`.
- **Deferred / depth-batched** ported: `DepthBatchedInserter`,
  `DeferredInsertBuffer` (+ write-backs, since a resolved record is a new
  instance), `DeferredInserter`, `DeferredGraph`, `DescendantValuePass`,
  `CopyFromDescendantExpression`. `PersistenceGatewayLike.insert` now returns
  the persisted records (same order). `RecordProvider.depthBatched()` +
  `InsertMode.DEFERRED`.
- **Enrichment** ported: `InjectConfig`, `BundleEnricher` (the recursive walk),
  `EnrichmentTarget/Selection/Position`, `ForcedValues`, `RecordInjector`,
  `InjectionPathResolver` (nav-prop by `<Name>Id` convention + `List<Child>`
  scan), `QueryableShapeValidator`, `PathKey`. `Bundle.inject*`.
- **Shared ancestors** ported: `SharedAncestor` (+ `SharedAncestorProvider`,
  `SharedAncestorResolver`, `SharedRelationshipWiring`). C#'s async reentrant
  gate → `ReentrantLock` (resolution is synchronous in practice; flagged).
- **`Xfty.Test` translation so far:** predicates (11), values (7 plain +
  `CopyFromSibling` + `ContextAwareExpression`), lookup (`LookupKeyTest`,
  `DiscriminatorLookupKeyTest`), `InverseAlignment`, `AncestorCycleGuard`,
  `IdMocker`, `XftyConfigurationException`, `DepthBatchedInserterTest`,
  `PersistenceGatewayTest`, plus new integration tests (`RecordProviderIntegrationTest`,
  `DeferredInsertIntegrationTest`, `EnrichmentIntegrationTest`,
  `SharedAncestorIntegrationTest`). **202 tests, 2 skipped (Docker), 0 failures.**

**Still to translate from `Xfty.Test`:** `Core/*` (BundleTest, MasterTemplateTest,
PathValueTest, PathTargetValueTest, GenerationContextTest, RecordProviderApiTest,
RecordProviderScenarioTest, BundleMergerTest, ChildProviderTest,
ChildProviderOfTTest, DeferredValueQueueTest, UnsetFieldFillerTest,
RecordProviderOfTTest), `Engine/*` (AncestorCycleTest, RecordFactoryTest),
`Enrichment/*` unit tests, `Examples/*` (the runnable-doc suite), `Demo/*`,
remaining `Relationships/*` (`SharedAncestorTest`, `SharedAncestorHierarchyTest`,
`SharedAncestorResetTest`, `DefaultRelationshipTest` is done), `Lookup/*`
(`MultiVariantProviderTest`, `VariantResolutionTest`), remaining `Values/*`
(`CopyFromAncestorExpressionTest`, `CopyFromDescendantExpressionTest`).
**Then:** `scripts/verify-doc-examples` equivalent + `docs/`; then the other
`Xfty.*` add-on modules.

**Tests:** `RecordProviderIntegrationTest` (11) - defaults, override-wins,
mock ids, `NEVER` leaves id unset, required-relationship FK wiring, `NONE`
skips it, quantity → N distinct, context-aware sibling (success + the loud
throw on a still-pending sibling), context-aware ancestor copy. 74 total,
green.

### Still to port

Children, shared ancestors, deferred/depth-batched persistence, `InsertMode.NOW`
against a real gateway, the typed wrappers, enrichment, `xfty-jpa`, and keyless
signing.
