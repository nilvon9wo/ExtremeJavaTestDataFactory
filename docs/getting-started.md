# Getting started

XFTY generates the object graphs your tests need from a small declarative
description, so a test states only what makes it unique.

## Add the dependency

```kotlin
// build.gradle.kts
testImplementation("io.github.nilvon9wo:xfty:<version>")
```

(and `io.github.nilvon9wo:xfty-jpa` if you want the real-database
`PersistenceGateway`.)

## Creating your first record

`supply()` is asynchronous - it returns `CompletableFuture<Contact>`. In a test,
`net.nowhereatall.xfty.Async.await(...)` blocks and unwraps any thrown
`XftyConfigurationException`:

```java
Contact result = Async.await(new RecordProvider<>(Contact.class, lookup)
    .supply());
```

No insert happens by default, so `result.id()` is `null` - the record lives
only in memory. Use `.setInsertMode(InsertMode.MOCK)` for a placeholder id, or
`InsertMode.NOW` with `.setPersistenceGateway(...)` for a real insert.

## Override templates

Set only the fields the test cares about; everything else comes from the
Provider's defaults.

```java
Contact contact = Async.await(new RecordProvider<>(Contact.class, lookup)
    .setOverrideTemplate(Contact.builder().firstName("Alice").lastName("Smith").build())
    .supply());
```

An override always wins over a Provider default or a `put(...)`.

## Shorthand constructors

```java
Contact fromTemplate = Async.await(
    new RecordProvider<>(Contact.builder().firstName("Alice").build(), lookup).supply());

List<Contact> fromList = Async.await(
    new RecordProvider<>(List.of(Contact.builder().build(), Contact.builder().build()), lookup).supplyList());

Contact fromKey = Async.await(
    new RecordProvider<Contact>(LookupKey.get(Contact.class), lookup).supply());
```

## Understanding bundles

`supplyBundle()` returns the whole generated graph, not just the primary
record - useful when a test needs to reach a generated parent:

```java
Bundle bundle = Async.await(new RecordProvider<>(Case.class, lookup)
    .setInsertMode(InsertMode.MOCK)
    .setInclusivity(InsertInclusivity.REQUIRED)
    .supplyBundle());

List<Account> accounts = bundle.getList(Account.class, Case.class, "accountId");
Bundle accountBundle = bundle.getBundle(Case.class, "accountId");
```

`InsertInclusivity` controls how far relationships are followed: `NONE` (the
default) generates no related records, `REQUIRED` fills in required
relationships, `ALL` fills optional ones too.

## Writing a Provider

A Provider is usually just a Master Template - extend `SimpleRecordProvider` and
pass one to `super(...)`. See
[`ContactDataProvider`](../xfty/src/main/java/net/nowhereatall/xfty/demo/ContactDataProvider.java)
for a worked example. Register your Providers in a `ProviderLookupLike` (copy
[`DefaultProviderLookup`](../xfty/src/main/java/net/nowhereatall/xfty/demo/DefaultProviderLookup.java)
as a starting point) and pass that to `new RecordProvider<>(...)`.

---

Every code block above is compiled and run as a test -
`xfty/src/test/java/net/nowhereatall/xfty/examples/`.
