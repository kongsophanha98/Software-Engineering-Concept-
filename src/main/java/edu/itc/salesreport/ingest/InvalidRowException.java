package edu.itc.salesreport.ingest;

/** Checked: a bad input row is an expected event, not a bug. */
public class InvalidRowException extends Exception {
    public InvalidRowException(String message) {
        super(message);
    }

    public InvalidRowException(String message, Throwable cause) {
        super(message, cause);
    }
}
