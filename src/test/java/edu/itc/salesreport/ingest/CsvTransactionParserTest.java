package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.*;

import edu.itc.salesreport.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvTransactionParserTest {

    private final TransactionParser parser = new CsvTransactionParser();

    @Test
    void parsesAValidRow() throws InvalidRowException {
        var t = parser.parse("PNH,2026-09-01,PNH-000123,"
                + "SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR");
        assertEquals("PNH", t.branch());
        assertEquals(LocalDate.of(2026, 9, 1), t.date());
        assertEquals("PNH-000123", t.receiptNo());
        assertEquals("SKU-1001", t.sku());
        assertEquals("Jasmine Rice 5kg", t.productName());
        assertEquals("Grocery", t.category());
        assertEquals(2, t.quantity());
        assertEquals(new BigDecimal("6.50"), t.unitPrice());
        assertEquals(PaymentMethod.KHQR, t.paymentMethod());
        assertEquals(new BigDecimal("13.00"), t.revenue());
    }

    @Test
    void parsesARowWithADiscount() throws InvalidRowException {
        var t = parser.parse("REP,2026-09-02,REP-000007,"
                + "SKU-5001,USB-C Cable 1m,Electronics,3,4.50,0.10,CARD");
        assertEquals(new BigDecimal("0.10"), t.discount());
        assertEquals(new BigDecimal("13.40"), t.revenue());
    }

    @Test
    void anEmptyLastFieldIsKeptAndRejected() {
        // split(",", -1) keeps the trailing empty field: 10 fields, bad payment
        var ex = assertThrows(InvalidRowException.class, () -> parser.parse(
                "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,"));
        assertTrue(ex.getMessage().contains("payment_method"));
    }

    @Test
    void theMessageNamesTheBadField() {
        var ex = assertThrows(InvalidRowException.class, () -> parser.parse(
                "REP,2026-09-02,REP-1,SKU-1,Rice,Grocery,two,6.50,0.00,CASH"));
        assertTrue(ex.getMessage().contains("quantity"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00",
        "PNH,2026-09-31,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "XXX,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Clothing,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,two,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,0,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,-0.50,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,13.01,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,PAYPAL"
    })
    void rejectsInvalidRows(String line) {
        assertThrows(InvalidRowException.class, () -> parser.parse(line));
    }
}
