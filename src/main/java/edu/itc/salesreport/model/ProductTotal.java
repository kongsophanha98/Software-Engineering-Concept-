package edu.itc.salesreport.model;

import java.math.BigDecimal;

/** Revenue and units sold for one product over a period. */
public record ProductTotal(String sku, String productName,
        BigDecimal revenue, long quantity) { }
