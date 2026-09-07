package net.nowhereatall.xfty.jpa;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.nowhereatall.xfty.jpa.demo.JpaAccount;
import net.nowhereatall.xfty.jpa.demo.JpaContact;
import org.hibernate.cfg.Configuration;

/** Builds a Hibernate-backed {@link EntityManagerFactory} for a JDBC URL, no {@code persistence.xml}. */
final class JpaTestSupport {

    private JpaTestSupport() {
    }

    static EntityManagerFactory entityManagerFactory(String jdbcUrl, String user, String password, String driverClass) {
        Configuration configuration = new Configuration();
        for (Class<?> entity : List.of(JpaAccount.class, JpaContact.class)) {
            configuration.addAnnotatedClass(entity);
        }
        configuration.setProperty("hibernate.connection.url", jdbcUrl);
        configuration.setProperty("hibernate.connection.username", user);
        configuration.setProperty("hibernate.connection.password", password);
        configuration.setProperty("hibernate.connection.driver_class", driverClass);
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("hibernate.show_sql", "false");
        return configuration.buildSessionFactory();
    }

    static EntityManagerFactory h2() {
        return entityManagerFactory(
                "jdbc:h2:mem:xfty-jpa-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
                "sa", "", "org.h2.Driver");
    }
}
