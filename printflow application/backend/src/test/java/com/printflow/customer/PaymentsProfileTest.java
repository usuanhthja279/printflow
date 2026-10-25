package com.printflow.customer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class PaymentsProfileTest {
    @Test void testReceiptsContributeToTotalsOnlyOutsideProductionProfile() {
        var developmentEnvironment = new MockEnvironment();
        developmentEnvironment.setActiveProfiles("dev");
        var productionEnvironment = new MockEnvironment();
        productionEnvironment.setActiveProfiles("prod");
        var development = new Payments(null, null, null, null, developmentEnvironment);
        var production = new Payments(null, null, null, null, productionEnvironment);

        assertTrue(development.testPaymentsIncludedInTotals());
        assertFalse(production.testPaymentsIncludedInTotals());
    }
}
