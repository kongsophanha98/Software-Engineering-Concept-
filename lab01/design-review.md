# Design review: cohesion and coupling of release 1

Counts come from the `import edu.itc.salesreport.*` lines of `src/main` (JDK imports and tests are not counted). Ce = distinct project packages a package imports; Ca = distinct project packages that import it; I = Ce / (Ca + Ce).

| Package | Responsibility (one sentence) | Ce | Ca | I = Ce / (Ca + Ce) |
|---------|-------------------------------|----|----|--------------------|
| `model` | The canonical records and their invariants (money rule, validation of a sale) and nothing else. | 0 | 2 (`ingest`, `render`) | 0 / (2 + 0) = **0.00** |
| `ingest` | Reads POS files, turns lines into `SaleTransaction`s, reports rejected rows and load progress. | 1 (`model`) | 1 (root `App`) | 1 / (1 + 1) = **0.50** |
| `render` | Turns a finished `MonthlyReport` into bytes of one output format (Strategy). | 1 (`model`) | 0 | 1 / (0 + 1) = **1.00** |
| `edu.itc.salesreport` (`App`) | Composition root: creates the objects and connects them. | 1 (`ingest`) | 0 | 1 / (0 + 1) = **1.00** |

Reading: dependencies point towards the stable `model` (I = 0), and the volatile edges (`render`, `App`, I = 1) depend on stable code, never the other way round. `DependencyRulesTest` keeps it so: `model` imports no other project package and `ingest` does not know `render`. All three packages are cohesive: each one has one reason to change (a business rule, an input format, an output format).

**One coupling I accept:** `ingest` and `render` both depend on `model`. It is the right place for coupling: records are small, stable and shared by design, and a change to `SaleTransaction` should be visible at compile time in every package that uses it.

**Sealed or open for QA-4?** I choose the **sealed** `ReportRenderer`. QA-4 measures a change in our own repository (at most 4 files, at most 1 person-day, 0 files in `model`/`ingest`): a new renderer is one new class plus one line in `permits` and one in `forExtension`. In return the compiler checks every `switch` over renderers for exhaustiveness, so a forgotten case becomes a compile error instead of a missing report. An open interface would only pay off if third parties had to add renderers without touching our code, which nobody has asked for; I would revisit this if Lab-08 (PDF, XLSX) brings plug-in style libraries.
