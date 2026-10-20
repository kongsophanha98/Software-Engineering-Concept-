package edu.itc.salesreport.ingest;

/** Observer of a MonthLoader. */
@FunctionalInterface
public interface LoadListener {
    void onEvent(LoadEvent event);
}
