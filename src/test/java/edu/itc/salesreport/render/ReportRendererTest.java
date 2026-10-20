package edu.itc.salesreport.render;

import static org.junit.jupiter.api.Assertions.*;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportRendererTest {

    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void everyFormatShowsEveryBranchAndTheChainTotal(String ext) {
        var renderer = ReportRenderer.forExtension(ext);
        assertEquals(ext, renderer.extension());
        String out = new String(renderer.render(TestReports.september()),
                StandardCharsets.UTF_8);
        for (String code : List.of("PNH", "REP", "BTB", "ALL")) {
            assertTrue(out.contains(code), ext + " lacks " + code);
        }
        assertTrue(out.contains("2300.50"), ext + " lacks the chain total");
    }

    @Test
    void csvHasAnExactLineForREP() {
        String csv = new String(new CsvReportRenderer().render(TestReports.september()),
                StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("month,branch,revenue,discounts,receipts,average_basket\n"));
        assertTrue(csv.contains("2026-09,REP,800.50,0.00,50,16.01\n"));
    }

    @Test
    void htmlEscapesSpecialCharacters() {
        var evil = new BranchSummary("A&B<x>", new BigDecimal("1.00"),
                new BigDecimal("0.00"), 1, Map.of(), List.of(), Map.of());
        var report = new MonthlyReport(YearMonth.of(2026, 9), List.of(evil), evil, List.of());
        String html = new String(new HtmlReportRenderer().render(report),
                StandardCharsets.UTF_8);
        assertTrue(html.contains("A&amp;B&lt;x&gt;"));
        assertFalse(html.contains("A&B<x>"));
    }

    @Test
    void unknownExtensionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReportRenderer.forExtension("pdf"));
    }

    @Test
    void anExhaustiveSwitchNeedsNoDefault() {
        // compiles only while the permitted set stays Text, Html, Csv
        for (String ext : List.of("txt", "html", "csv")) {
            ReportRenderer r = ReportRenderer.forExtension(ext);
            String name = switch (r) {
                case TextReportRenderer t -> "text";
                case HtmlReportRenderer h -> "html";
                case CsvReportRenderer c -> "csv";
            };
            assertFalse(name.isEmpty());
        }
    }
}
