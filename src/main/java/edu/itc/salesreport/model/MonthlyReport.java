package edu.itc.salesreport.model;

import java.time.YearMonth;
import java.util.List;

/** The finished report for one month: every branch, the chain, anomalies. */
public record MonthlyReport(YearMonth month, List<BranchSummary> branches,
        BranchSummary chain, List<String> anomalies) {

    public MonthlyReport {
        branches = List.copyOf(branches);
        anomalies = List.copyOf(anomalies);
    }
}
