# C4 notes: Angkor Mart monthly sales report

## Context diagram (level 1)
- **Deliberately not shown:** the inside of the system (containers, packages, classes), the technology of the POS systems, and how the e-mail service is hosted; the finance director appears only as the receiver of the e-mail.
- **Quality-attribute link:** the arrow "uploads daily CSV files" from the POS systems is the source of the QA-3 (availability) scenario, because that link is the one that fails; the "views own branch report" arrow carries QA-5 (security).
- **Open question for the client:** may head office read a *partial* report (one branch missing, clearly marked) at 07:30, or must it wait for the full set of files?

## Container diagram (level 2)
- **Deliberately not shown:** the packages `model`, `ingest`, `engine`, `render` inside the two Java batches, the three renderer strategies, the host or server they run on, backups and monitoring.
- **Quality-attribute link:** the *Landing zone* together with the *Ingest job* serves QA-3 (files survive a link failure and a late file can be loaded and re-run without duplicates); the *Report batch* serves QA-1 (parallel parse and aggregation of 3 M rows in 60 s or less) and, through its renderers, QA-4; the *Web report page* serves QA-5 (one branch per login).
- **Open question for the client:** may the web page and the desktop viewer share one report store on the same server, or must the branch managers' page run on a separate machine for security?
