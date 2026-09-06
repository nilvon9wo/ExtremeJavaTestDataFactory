package net.nowhereatall.xfty.predicates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import net.nowhereatall.xfty.Field;
import net.nowhereatall.xfty.demo.Account;
import org.junit.jupiter.api.Test;

class FieldPredicatesTest {

    private static Account account(String industry, int employees, BigDecimal revenue) {
        Account account = new Account();
        account.setIndustry(industry);
        account.setNumberOfEmployees(employees);
        account.setAnnualRevenue(revenue);
        return account;
    }

    @Test
    void equalToMatchesAFieldValue() {
        // Arrange
        RecordPredicateLike isTech = FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology");

        // Act / Assert
        assertTrue(isTech.isSatisfiedBy(account("Technology", 0, null)));
        assertFalse(isTech.isSatisfiedBy(account("Finance", 0, null)));
    }

    @Test
    void equalToWithNullIsAnIsNullCheck() {
        // Arrange
        RecordPredicateLike hasNoIndustry = FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), null);

        // Act / Assert
        assertTrue(hasNoIndustry.isSatisfiedBy(account(null, 0, null)));
    }

    @Test
    void greaterThanComparesNumbersNumerically() {
        // Arrange
        RecordPredicateLike bigRevenue = FieldPredicateFactory.greaterThan(
                Field.of(Account.class, "annualRevenue"), new BigDecimal("1000000"));

        // Act / Assert
        assertTrue(bigRevenue.isSatisfiedBy(account(null, 0, new BigDecimal("2500000"))));
        assertFalse(bigRevenue.isSatisfiedBy(account(null, 0, new BigDecimal("500000"))));
    }

    @Test
    void greaterThanIsNeverTrueForANullFieldValue() {
        // Arrange
        RecordPredicateLike bigRevenue = FieldPredicateFactory.greaterThan(
                Field.of(Account.class, "annualRevenue"), new BigDecimal("1"));

        // Act / Assert
        assertFalse(bigRevenue.isSatisfiedBy(account(null, 0, null)));
    }

    @Test
    void inSetMatchesAnyAcceptedValue() {
        // Arrange
        RecordPredicateLike favoured = FieldPredicateFactory.inSet(
                Field.of(Account.class, "industry"), List.of("Technology", "Healthcare"));

        // Act / Assert
        assertTrue(favoured.isSatisfiedBy(account("Healthcare", 0, null)));
        assertFalse(favoured.isSatisfiedBy(account("Retail", 0, null)));
    }

    @Test
    void negateInvertsThePredicate() {
        // Arrange
        RecordPredicateLike notTech = PredicateFactory.negate(
                FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"));

        // Act / Assert
        assertTrue(notTech.isSatisfiedBy(account("Finance", 0, null)));
        assertFalse(notTech.isSatisfiedBy(account("Technology", 0, null)));
    }

    @Test
    void allOfRequiresEveryMember() {
        // Arrange
        RecordPredicateLike bigTech = PredicateFactory.allOf(List.of(
                FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"),
                FieldPredicateFactory.greaterThan(Field.of(Account.class, "numberOfEmployees"), 100)));

        // Act / Assert
        assertTrue(bigTech.isSatisfiedBy(account("Technology", 5000, null)));
        assertFalse(bigTech.isSatisfiedBy(account("Technology", 10, null)));
    }

    @Test
    void anyOfRequiresAtLeastOneMember() {
        // Arrange
        RecordPredicateLike techOrHuge = PredicateFactory.anyOf(List.of(
                FieldPredicateFactory.equalTo(Field.of(Account.class, "industry"), "Technology"),
                FieldPredicateFactory.greaterThan(Field.of(Account.class, "numberOfEmployees"), 10000)));

        // Act / Assert
        assertTrue(techOrHuge.isSatisfiedBy(account("Finance", 50000, null)));
        assertFalse(techOrHuge.isSatisfiedBy(account("Finance", 10, null)));
    }
}
