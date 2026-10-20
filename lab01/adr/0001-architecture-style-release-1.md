# ADR-0001: Build release 1 as a modular monolith with two batch jobs

Status: Accepted
Date: 2026-10-19 · Deciders: TODO name A, TODO name B (reviewed by TODO name B)

## Context
The client needs a monthly sales report for 3 branches (about 3 M rows per month, 33 000 rows per branch per day) on the morning of day 2 of the next month. Three of our scenarios shape the structure (the architectural drivers of `quality-scenarios.md`):
- QA-1 (H,H): a month of 3 M rows is processed in 60 s or less (median of 5 runs after 2 warm-up runs).
- QA-3 (H,M): when a branch file arrives late, a marked partial report is out by 07:30 and the full report follows within 15 minutes of the late file.
- QA-5 (H,M): a branch manager sees only the own branch; 0 of 50 crafted requests may leak another branch.
Constraints: a team of two, eight weeks of labs, skills in Java and Maven, no operations staff at the client, one 8-core server (assumption A1).

## Decision
We will build release 1 as one Maven project `sales-report` (a modular monolith with the packages `model`, `ingest`, `engine`, `render`, `ui`) that runs as two batch jobs from the same code base: an ingest job after the daily files arrive and a report batch at 06:00 on day 2. Parsing and aggregation inside the report batch run in parallel threads of one JVM. Files and a report store on the local file system are the only shared state; the web page and the desktop viewer only read finished reports.

## Alternatives considered
- **B: process pipeline** (a coordinator and worker JVMs connected by pipes or local sockets): attractive because a crashing worker does not kill the others and the work can later spread over several machines (QA-2). Not chosen: for 3 M rows one JVM needs about 5 s (see `atam.md`), so processes buy nothing for QA-1 today, and they add start-up, serialization and test effort.
- **C: event-driven microservices** (ingest, aggregation, report and notification services around a message broker): attractive because the broker buffers late data (QA-3) and every service scales alone (QA-2). Not chosen: one event per row would need 3.0 M / 50 000 msg/s = 60 s of broker time alone and break QA-1; it needs a broker to operate, more endpoints to secure (QA-5) and more skills than we have in eight weeks.
- **Maven modules from day one** (one artifact per package): attractive for enforcing boundaries. Not chosen: `DependencyRulesTest` already enforces the two important rules at much lower cost; we can split the project later.

## Consequences
+ QA-1: no serialization or network hop; about 4.9 s estimated for 3 M rows against a 60 s budget, so a large safety margin.
+ QA-3: validated files stay on disk in the landing zone; a late file means "run the same job again", which is cheap and testable.
+ QA-5: one deployable and one perimeter (the web page); there are few places where an authorisation check can be forgotten.
+ QA-6: the engine is plain Java, so tests need no broker, no containers and no network.
- QA-2: scaling is vertical (more cores, faster disk); one JVM and one server cap the size of a month.
- QA-3: the server is a single point of failure; if it is down at 06:00 nothing is produced until it is back.
- QA-1: all rows share one JVM heap; a careless implementation that keeps every `SaleTransaction` (about 750 MB for 3 M rows at roughly 250 bytes per object) could hit memory limits, so aggregation must stream.
- Boundaries between modules are enforced by tests and discipline, not by the build tool.

Revisit when: a month exceeds 10 M rows, the QA-1 run takes longer than 30 s (half the budget), or the chain has more than 8 branches.
