package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;
import java.nio.charset.StandardCharsets;

/** Web page for branch managers: one table row per branch plus the chain. */
public final class HtmlReportRenderer implements ReportRenderer {
    @Override
    public String extension() {
        return "html";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\">")
          .append("<title>Angkor Mart sales ").append(escape(report.month().toString()))
          .append("</title></head><body>\n");
        sb.append("<h1>Angkor Mart monthly sales ")
          .append(escape(report.month().toString())).append("</h1>\n");
        sb.append("<table border=\"1\">\n<thead><tr><th>Branch</th><th>Revenue</th>")
          .append("<th>Receipts</th><th>Average basket</th></tr></thead>\n<tbody>\n");
        for (BranchSummary b : report.branches()) {
            row(sb, b);
        }
        row(sb, report.chain());
        sb.append("</tbody>\n</table>\n</body></html>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, BranchSummary b) {
        sb.append("<tr><td>").append(escape(b.branch()))
          .append("</td><td>").append(b.revenue())
          .append("</td><td>").append(b.receipts())
          .append("</td><td>").append(b.averageBasket())
          .append("</td></tr>\n");
    }

    /** Escapes &, < and > (the & first, so it is not double-escaped). */
    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
