package net.nowhereatall.xfty.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.nowhereatall.xfty.Async;
import net.nowhereatall.xfty.core.InsertInclusivity;
import net.nowhereatall.xfty.core.InsertMode;
import net.nowhereatall.xfty.core.RecordProvider;
import net.nowhereatall.xfty.jpa.demo.JpaAccount;
import net.nowhereatall.xfty.jpa.demo.JpaContact;
import net.nowhereatall.xfty.jpa.demo.JpaDemoProviders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Proves {@code InsertMode.NOW} against a real client-server database - a
 * Postgres container started with Docker via Testcontainers - not a mock, and
 * not H2's in-process engine. Skips (rather than fails) when Docker is not
 * reachable, so this tier is opt-in on a developer machine and runs
 * automatically on any CI runner with Docker (GitHub Actions' ubuntu-latest has
 * it out of the box).
 */
@Tag("docker")
class PostgresNowPersistenceTest {

    private static PostgreSQLContainer<?> container;
    private static boolean dockerAvailable;

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeAll
    static void startContainer() {
        dockerAvailable = isDockerAvailable();
        if (!dockerAvailable) {
            return;
        }
        container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
    }

    @AfterAll
    static void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void openDatabase() {
        assumeTrue(dockerAvailable, "Docker is not reachable - start Docker to run this tier.");
        this.entityManagerFactory = JpaTestSupport.entityManagerFactory(
                container.getJdbcUrl(), container.getUsername(), container.getPassword(), "org.postgresql.Driver");
        this.entityManager = this.entityManagerFactory.createEntityManager();
        this.entityManager.getTransaction().begin();
    }

    @AfterEach
    void closeDatabase() {
        if (this.entityManager != null) {
            if (this.entityManager.getTransaction().isActive()) {
                this.entityManager.getTransaction().rollback();
            }
            this.entityManager.close();
            this.entityManagerFactory.close();
        }
    }

    @Test
    void supply_InNowMode_AgainstARealPostgresContainer_ActuallyInsertsARow() {
        // Arrange
        RecordProvider<JpaAccount> provider = new RecordProvider<>(JpaAccount.class, JpaDemoProviders.lookup())
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(new JpaPersistenceGateway(this.entityManager));

        // Act
        JpaAccount result = Async.await(provider.supply());

        // Assert
        assertNotNull(result.getId());
        this.entityManager.clear();
        assertNotNull(this.entityManager.find(JpaAccount.class, result.getId()));
    }

    @Test
    void supplyBundle_InNowMode_AgainstARealPostgresContainer_WiresTheRealForeignKey() {
        // Arrange
        RecordProvider<JpaContact> provider = new RecordProvider<>(JpaContact.class, JpaDemoProviders.lookup())
                .setInclusivity(InsertInclusivity.REQUIRED)
                .setInsertMode(InsertMode.NOW)
                .setPersistenceGateway(new JpaPersistenceGateway(this.entityManager));

        // Act
        JpaContact result = Async.await(provider.supply());

        // Assert
        this.entityManager.clear();
        JpaContact rereadContact = this.entityManager.find(JpaContact.class, result.getId());
        JpaAccount rereadAccount = this.entityManager
                .createQuery("select a from JpaAccount a", JpaAccount.class).getResultList().get(0);
        assertEquals(rereadAccount.getId(), rereadContact.getAccountId());
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException notAvailable) {
            return false;
        }
    }
}
