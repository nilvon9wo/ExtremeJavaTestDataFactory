Java prompt (revised)

Port XFTY's core (github.com/nilvon9wo/ExtremeCSharpTestDataFactory) to
Java, INCLUDING a real persistence extension from day 1. Read that
repo's Xfty/ folder as primary reference, and Xfty.EntityFrameworkCore/
Xfty.EntityFrameworkCore.Test as the direct template for the persistence
module described below - mirror its shape closely. Apex original and
porting-history.md are there if something's unclear.

SCOPE: core generation engine (Provider/lookup/relationship/value-
expression/persistence-seam) PLUS one real persistence binding (below).
Do NOT port Xfty.AutoFixture/Bogus/Xunit equivalents this pass - see
"Later" at the end.

DECISIONS ALREADY MADE:

1. Field tokens - support BOTH, not one:
   - `Field.of(Account.class, "name")` - string-based, always works,
     any class, zero requirements. The universal fallback.
   - `Field.of(Account::getName)` - preferred when usable: a
     Serializable functional interface (define your own
     SerializableFunction<T,R> extends Function<T,R>, Serializable),
     decomposed via SerializedLambda reflection to recover the target
     method name, then mapped to a field/property. Handle BOTH naming
     conventions when mapping the recovered method name: JavaBean
     getters (getName/isActive -> name/active) AND Java record
     accessors (bare name() -> name, since records don't use a "get"
     prefix). Throw a clear, named exception immediately if the
     reference can't be cleanly decomposed into a plain accessor call
     (e.g. a lambda body doing more than one thing) - point the
     exception message at the string-based overload as the fallback.

2. Java `record` support is REQUIRED in v1, fully co-equal with classic
   mutable classes - not staged, not assumed-away in favor of "you're
   probably using JPA anyway." Detect via Class.isRecord(). For records,
   every place the C# port reflectively SETS a field after construction
   (IdMocker, RecordCloneFactory, RecordInjector) instead: read every
   current value via its RecordComponent accessor, override the one
   that changed, reconstruct via the canonical constructor with the new
   argument array. For classic mutable classes, direct field mutation
   via setAccessible(true) + Field.set, same as the C# port's design.
   One dispatch point choosing the strategy per Class.isRecord(), not
   two divergent code paths through the rest of the engine.

3. Lookup/variant/specificity system (Xfty/Lookup/*): port close to 1:1
   - Java's real Class<T> identity via getClass() gives the same exact-
   type-equality guarantee C#'s GetType() does, so the specificity tie-
   break needs no redesign.

4. Persistence extension - xfty-jpa module, in scope for this pass:
   implement the ported PersistenceGateway interface via
   EntityManager.persist(...), proven against a real database, mirroring
   Xfty.EntityFrameworkCore.Test's two-tier pattern exactly: an H2
   embedded in-memory tier that always runs (no Docker), plus a real
   Postgres tier via Testcontainers, tagged to skip rather than fail
   without Docker. Important, expected constraint, not a gap to solve:
   JPA entities cannot be Java records (the spec requires a mutable, no-
   arg-constructible class) - this module's own demo/test entities are
   necessarily classic mutable classes. That's fine; it doesn't limit
   what core generation supports, only what this one persistence binding
   can target.

5. Build/test: Gradle + JUnit5, not Maven. Mirror this project's testing
   conventions as closely as idiomatic Java allows - one test class per
   unit, one behaviour per test method, AAA comments kept LITERAL
   (// Arrange, // Act, // Assert, // Sanity Check), naming adapted to
   Java convention otherwise.

6. Package naming: propose net.nowhereatall.xfty.

7. Publish-ready, for real: Set up the project for publication to Maven Central, 
   with the appropriate /Gradle metadata, compiled JAR artifacts, source JAR, 
   and Javadoc JAR. Check whether the bare xfty artifact name is available; 
   use a group/artifact coordinate such as net.nowhereatall:xfty
   (matching this port's existing placeholder-domain branding) if appropriate. 
   Set up publishing via Maven Central's OIDC-based Trusted Publishing rather 
   than a stored repository token, with the necessary GitHub Actions OIDC permissions — 
   deliberately following the same approach and reasoning already used for this project's 
   NuGet publish workflow, not a new decision to make.   

LATER (not this pass, just don't paint into a corner): Java equivalents
of the other C# add-ons, if/when wanted - DataFaker (Bogus equivalent),
Instancio or EasyRandom (AutoFixture equivalent), a JUnit5 extension
(@ExtendWith) for the SharedAncestor-reset attribute equivalent.

PROCESS: build and run real tests as you go, including the H2-backed
persistence tier - don't reason about correctness from the code alone
when you can execute it. Set up a new repository (ask where). Check in
once the reflection/field-token layer, the lookup/variant/specificity
system, and the record-vs-bean mutation dispatch are built and genuinely
tested - if that's solid, keep going through the rest (including
xfty-jpa) without waiting for further sign-off.