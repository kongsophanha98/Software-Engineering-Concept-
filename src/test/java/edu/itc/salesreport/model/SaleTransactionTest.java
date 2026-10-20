package edu.itc.salesreport.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaleTransactionTest {

    private static SaleTransaction sale(int qty, String price, String discount) {
        return new SaleTransaction("PNH", LocalDate.of(2026, 9, 1), "PNH-000001",
                "SKU-1001", "Jasmine Rice 5kg", "Grocery", qty,
                new BigDecimal(price), new BigDecimal(discount), PaymentMethod.KHQR);
    }

    @Test
    void revenueIsRoundedHalfUpToScaleTwo() {
        // 3 x 0.335 = 1.005 -> 1.01 (HALF_UP); a double would give 1.00
        assertEquals(new BigDecimal("1.01"), sale(3, "0.335", "0.00").revenue());
    }

    @Test
    void revenueSubtractsTheDiscountAndKeepsScaleTwo() {
        assertEquals(new BigDecimal("12.90"), sale(2, "6.50", "0.10").revenue());
        assertEquals(new BigDecimal("13.00"), sale(2, "6.50", "0").revenue());
    }

    @Test
    void invariantsAreEnforcedByTheConstructor() {
        assertThrows(IllegalArgumentException.class, () -> sale(0, "6.50", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> sale(1, "-1.00", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> sale(1, "6.50", "-0.01"));
        assertThrows(IllegalArgumentException.class, () -> sale(1, "6.50", "6.51"));
    }

    @Test
    void topProductsIsUnmodifiable() {
        var mutable = new java.util.ArrayList<ProductTotal>();
        mutable.add(new ProductTotal("SKU-1001", "Jasmine Rice 5kg",
                new BigDecimal("13.00"), 2));
        var summary = new BranchSummary("PNH", new BigDecimal("13.00"),
                new BigDecimal("0.00"), 1, Map.of(), mutable, Map.of());
        assertThrows(UnsupportedOperationException.class,
                () -> summary.topProducts().add(null));
        mutable.clear();                                   // later change of the source ...
        assertEquals(1, summary.topProducts().size());     // ... does not leak in
    }

    @Test
    void averageBasketIsZeroWithoutReceipts() {
        var empty = new BranchSummary("PNH", new BigDecimal("0.00"),
                new BigDecimal("0.00"), 0, Map.of(), List.of(), Map.of());
        assertEquals(new BigDecimal("0.00"), empty.averageBasket());
        var one = new BranchSummary("PNH", new BigDecimal("800.50"),
                new BigDecimal("0.00"), 50, Map.of(), List.of(), Map.of());
        assertEquals(new BigDecimal("16.01"), one.averageBasket());
    }
}
