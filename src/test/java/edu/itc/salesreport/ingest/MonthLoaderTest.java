package edu.itc.salesreport.ingest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonthLoaderTest {

    private static final String HEADER = CsvTransactionParser.HEADER;
    private static final String GOOD = "PNH,2026-09-01,PNH-000001,SKU-1001,"
            + "Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR";
    private static final String BAD = "PNH,2026-09-01,PNH-000002,SKU-1001,"
            + "Jasmine Rice 5kg,Grocery,two,6.50,0.00,KHQR";

    private static void write(Path dir, String name, String... rows) throws IOException {
        var lines = new ArrayList<String>();
        lines.add(HEADER);
        lines.addAll(List.of(rows));
        Files.write(dir.resolve(name), lines);
    }

    @Test
    void publishesOneEventPerFilePlusFinalEventInOrder(@TempDir Path tmp) throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("2026-09"));
        write(dir, "PNH-2026-09-01.csv", GOOD, GOOD);
        write(dir, "PNH-2026-09-02.csv", GOOD, BAD);

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        loader.subscribe(events::add);
        var rows = loader.load(YearMonth.of(2026, 9), tmp);

        assertEquals(3, events.size());
        var first = assertInstanceOf(LoadEvent.FileLoaded.class, events.get(0));
        assertEquals("PNH-2026-09-01.csv", first.file().getFileName().toString());
        assertEquals(2, first.accepted());
        assertEquals(0, first.rejected());
        assertEquals(1, first.filesDone());
        assertEquals(2, first.filesTotal());
        var second = assertInstanceOf(LoadEvent.FileLoaded.class, events.get(1));
        assertEquals(1, second.accepted());
        assertEquals(1, second.rejected());
        var last = assertInstanceOf(LoadEvent.LoadFinished.class, events.get(2));
        assertEquals(3, last.accepted());
        assertEquals(1, last.rejected());
        assertEquals(3, rows.size());
        // header is line 1, so the bad row (list index 2) is line 3
        assertEquals(1, loader.rejectedRows().size());
        assertTrue(loader.rejectedRows().get(0).startsWith("PNH-2026-09-02.csv:3 "));
    }

    @Test
    void anUnsubscribedListenerReceivesNothing(@TempDir Path tmp) throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("2026-09"));
        write(dir, "PNH-2026-09-01.csv", GOOD);

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        Runnable unsubscribe = loader.subscribe(events::add);
        unsubscribe.run();
        loader.load(YearMonth.of(2026, 9), tmp);

        assertTrue(events.isEmpty());
    }
}
