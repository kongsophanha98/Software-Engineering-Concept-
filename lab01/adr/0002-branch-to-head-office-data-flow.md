# ADR-0002: Branches push one daily file by SFTP; head office loads idempotently

Status: Accepted
Date: 2026-10-19 · Deciders: TODO name A, TODO name B (reviewed by TODO name A)

## Context
Every branch POS system exports one CSV file per day (`BRANCH-yyyy-MM-dd.csv`, about 33 000 rows) that arrives by 23:00 over the branch's own internet link; the links fail now and then. Head office wants the report on the morning of day 2. We must decide how the data travels from three branches to head office and what happens when a file is late (QA-3, H,M), while the report still meets QA-1 and each branch file is reachable only by its own branch (QA-5, H,M). The brief gives no way for head office to open connections into the branches; whether the branch networks allow inbound connections is unknown.

## Decision
Each branch **pushes** its finished daily file by SFTP (key-based login, one account per branch, write access to its own folder only) to the landing zone at head office. Head office never reaches into a branch. The ingest job validates each file and records its SHA-256 checksum.

**Failure policy (QA-3):**
1. **Waiting:** the report batch starts at 06:00 on day 2. If a file is missing it waits up to **90 minutes** (until 07:30), checking every 10 minutes.
2. **Partial report:** at 07:30 it publishes a report of the branches that arrived, with every page marked "PARTIAL: BTB 2026-09-30 missing", and e-mails the finance director and the operator.
3. **Late file:** when the missing file arrives and validates, the report batch re-runs for the month automatically and publishes version 2 within **15 minutes**; version 1 stays in the report store as history; the finance director gets a "complete" e-mail.
4. **Idempotent re-run:** the key of a file is its name `BRANCH-yyyy-MM-dd.csv` plus the SHA-256 of its content. The same name with the same checksum is ignored (no-op); the same name with a different checksum is a correction and **replaces** the old file (the old one is archived); rows are never appended, so a file can never be counted twice. A report is a pure function of the files in the landing zone, so running it twice gives identical numbers.

## Alternatives considered
- **Head office pulls from each branch** (SFTP or HTTPS client at head office): attractive because head office controls timing and can retry. Not chosen: it needs inbound access to three branch networks (unknown, probably blocked) and one credential per branch stored at head office; a down link blocks the pull in the same way as it blocks a push.
- **POS publishes one event per receipt to a message broker:** attractive because data flows continuously and nothing waits for 23:00. Not chosen: a broker to operate, a change in every POS system we do not control, and per-event processing for 3 M rows conflicts with QA-1 (see ADR-0001, alternative C).
- **Upload by HTTPS to a web endpoint:** attractive because it needs no extra server. Not chosen for release 1: the POS exports files today, SFTP clients are standard, and an extra public web endpoint enlarges the attack surface (QA-5).

## Consequences
+ QA-3: a link failure only delays one file; the landing zone and the checksum rule make a late or repeated upload harmless (no duplicates), and head office sees a clear marked partial report.
+ QA-5: per-branch accounts and folders mean a branch cannot read or overwrite another branch's files; no inbound access to the branches is needed.
+ QA-1: one file per day keeps the load simple; no per-row messages and no broker.
+ Simple to operate: files are visible and can be fixed by hand.
- QA-3: data is at most one day old; if a branch needs the report earlier than 23:00 plus processing, this flow cannot deliver it.
- QA-3: the policy introduces a rule head office must accept (a partial report exists); a wrong "PARTIAL" mark could be missed by a reader who only looks at a total.
- QA-5: we must manage and rotate three SFTP keys and protect the SFTP server.
- A corrected file silently replaces the old one; we depend on the archive and the log to explain a changed number.

Revisit when: a branch needs same-day figures, a branch gets a reliable always-on link and a broker is justified by ADR-0001's trigger, or more than one file per branch per day is required.
