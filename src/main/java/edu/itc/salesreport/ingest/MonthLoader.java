package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/** The subject: loads a month of CSV files and tells its observers. */
public final class MonthLoader {

    private final TransactionParser parser;
    private final List<LoadListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> rejectedRows = new ArrayList<>();

    public MonthLoader(TransactionParser parser) {
        this.parser = parser;
    }

    /** Registers a listener; the returned handle unsubscribes it. */
    public Runnable subscribe(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public List<String> rejectedRows() {
        return List.copyOf(rejectedRows);
    }

    public List<SaleTransaction> load(YearMonth month, Path dataDir) {
        Instant start = Instant.now();
        rejectedRows.clear();
        List<Path> files = listCsv(dataDir.resolve(month.toString()));
        List<SaleTransaction> all = new ArrayList<>();
        int totalOk = 0;
        int totalBad = 0;
        int done = 0;
        for (Path file : files) {
            int ok = 0;
            int bad = 0;
            List<String> lines = readLines(file);
            for (int i = 1; i < lines.size(); i++) {          // index 0 = header
                try {
                    all.add(parser.parse(lines.get(i)));
                    ok++;
                } catch (InvalidRowException e) {
                    bad++;
                    // list index i is line i + 1 (1-based, header is line 1)
                    rejectedRows.add(file.getFileName() + ":" + (i + 1)
                            + " " + e.getMessage());
                }
            }
            totalOk += ok;
            totalBad += bad;
            done++;
            publish(new LoadEvent.FileLoaded(file, ok, bad, done, files.size()));
        }
        publish(new LoadEvent.LoadFinished(totalOk, totalBad,
                Duration.between(start, Instant.now())));
        return List.copyOf(all);
    }

    private static List<Path> listCsv(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".csv")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void publish(LoadEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }
}
