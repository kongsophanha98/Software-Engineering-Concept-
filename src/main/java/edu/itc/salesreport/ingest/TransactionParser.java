package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

/** Turns one text line into a validated transaction. */
public interface TransactionParser {
    SaleTransaction parse(String line) throws InvalidRowException;
}
