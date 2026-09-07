package net.nowhereatall.xfty.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.Bundle;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.jpa.demo.JpaAccount;
import net.nowhereatall.xfty.jpa.demo.JpaContact;
import net.nowhereatall.xfty.jpa.demo.JpaDemoProviders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves {@code InsertMode.NOW} against a real database - an in-memory H2, not a
 * mock. No Docker, no external service; this tier always runs. See
 * {@code PostgresNowPersistenceTest} for the Docker-backed tier proving the same
 * thing against a real client-server database.
 */
class H2NowPersistenceTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void openDatabase() {
        this.entityManagerFactory = JpaTestSupport.h2();
        this.entityManager = this.entityManagerFactory.createEntityManager();
        this.entityManager.getTransaction().begin();
    }

    @AfterEach
    void closeDatabase() {
        if (this.entityManager.getTransaction().isActive()) {
            this.entityManager.getTransaction().rollback();
        }
        this.entityManager.close();
        this.entityManagerFactory.close();
    }

    private RecordProvider<JpaAccount> accountProvider() {
        return new RecordProvider<>(JpaAccount.class, JpaDemoProviders.lookup())
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(new JpaPersistenceGateway(this.entityManager));
    }

    private RecordProvider<JpaContact> contactProvider() {
        return new RecordProvider<>(JpaContact.class, JpaDemoProviders.lookup())
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(new JpaPersistenceGateway(this.entityManager));
    }

    @Test
    void supply_InNowMode_ActuallyInsertsARowIntoTheDatabase() {
        // Arrange
        RecordProvider<JpaAccount> provider = accountProvider();

        // Act
        JpaAccount result = Async.await(provider.supply());

        // Assert - not just an in-memory id: a real row is there for a fresh query to find
        assertNotNull(result.getId());
        this.entityManager.clear();
        JpaAccount reread = this.entityManager.find(JpaAccount.class, result.getId());
        assertNotNull(reread);
        assertEquals(result.getName(), reread.getName());
    }

    @Test
    void supplyBundle_InNowMode_InsertsTheRequiredParentRowToo() {
        // Arrange
        RecordProvider<JpaContact> provider = contactProvider().setInclusivity(InsertInclusivity.REQUIRED);

        // Act
        Bundle bundle = Async.await(provider.supplyBundle());

        // Assert
        JpaContact contact = bundle.getPrimaries(JpaContact.class).get(0);
        JpaAccount account = bundle.getList(JpaAccount.class, JpaContact.class, "accountId").get(0);
        assertEquals(1L, count("JpaAccount"));
        assertEquals(1L, count("JpaContact"));
        this.entityManager.clear();
        JpaContact rereadContact = this.entityManager.find(JpaContact.class, contact.getId());
        assertEquals(account.getId(), rereadContact.getAccountId());
        assertEquals(contact.getId(), rereadContact.getId());
    }

    @Test
    void supplyList_InNowMode_WithQuantity_InsertsEveryRow() {
        // Arrange
        RecordProvider<JpaAccount> provider = accountProvider().setQuantityPerTemplate(5);

        // Act
        List<JpaAccount> results = Async.await(provider.supplyList());

        // Assert
        assertEquals(5, results.size());
        assertEquals(5L, count("JpaAccount"));
    }

    private long count(String entityName) {
        return this.entityManager.createQuery("select count(e) from " + entityName + " e", Long.class).getSingleResult();
    }
}
