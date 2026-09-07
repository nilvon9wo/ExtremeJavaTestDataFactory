# Child records

Downward generation - an Account *with* its Contacts - is the mirror of the
usual upward "a Contact needs an Account".

```java
Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookup)
    .setInsertMode(InsertMode.MOCK)
    .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("Buyer").build()).setQuantity(3))
    .supplyBundle());

Account account = bundle.getPrimaries(Account.class).get(0);
List<Contact> contacts = bundle.getChildList(Contact.class, Contact.class, "accountId");
// contacts.size() == 3, each contact.accountId() == account.getId()
```

## ChildProvider

`ChildProvider` names the child's foreign-key field; the child type comes from
that field's declaring type.

```java
ChildProvider blank        = new ChildProvider(Field.of(Contact.class, "accountId"));
ChildProvider withTemplate = new ChildProvider(Field.of(Contact.class, "accountId"),
                                               Contact.builder().department("Buyer").build());
```

`ChildProvider.forField(Contact::accountId, template)` is the method-reference
form. `.setQuantity(n)` sets how many children per parent (default 1).

## Attaching it

`with(...)` is additive and can mix child types:

```java
Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookup)
    .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("A").build()).setQuantity(3))
    .with(ChildProvider.forField(Contact::accountId, Contact.builder().department("B").build()).setQuantity(2))
    .with(ChildProvider.forField(Case::getAccountId, new Case()).setQuantity(2))
    .setInsertMode(InsertMode.MOCK)
    .supplyBundle());

// bundle.getChildList(Contact.class, Contact.class, "accountId").size() == 5
// bundle.getChildList(Case.class, Case.class, "accountId").size() == 2
```

## Grandchildren

`ChildProvider` nests:

```java
Bundle bundle = Async.await(new RecordProvider<>(Account.class, lookup)
    .setInsertMode(InsertMode.MOCK)
    .with(ChildProvider.forField(Contact::accountId, Contact.builder().build()).setQuantity(3)
        .with(ChildProvider.forField(Case::getContactId, new Case()).setQuantity(2)))
    .supplyBundle());

List<Case> cases = bundle.getChildBundle(Contact.class, "accountId")
    .getChildList(Case.class, Case.class, "contactId");
// cases.size() == 6  (3 contacts x 2 cases)
```

---

Examples verified by `ExChildRecordsTest`.
