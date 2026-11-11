# Demo walkthrough (local)

## 1. Start with the service overview

Launch `run.ps1` and visit `http://localhost:8080`. The dashboard shows 30 seeded IT requests, 7 sample users, open incidents and ticket assignments. Explain that everything is synthetic and reproducible, with no external service dependency.

## 2. Work the priority queue

Open **Tickets**, filter by priority **Urgent**, select a ticket, and inspect its due date, assignee, description and audit trail. Switch to the **Technician** demo role and change its status to *In progress* or *Resolved*.

## 3. Assign work as an administrator

Switch to **Admin**, select a ticket and assign a technician. The assignment is persisted in `data/runtime/tickets.tsv` and recorded in `data/runtime/events.tsv`. Reload the browser to demonstrate persistence.

## 4. Submit a support request

Click **New ticket**, enter a meaningful subject (at least 5 characters), a description (at least 10), category and priority. Open the created ticket and add a comment. Visit **Reports** to see refreshed counts and export a CSV of ticket records.

## 5. Discuss security boundaries and architecture

The service checks demo role headers on protected endpoints, but does not authenticate real people. In a production system, replace the demo headers with server-side identity and access control, use a proper transactional database, a business-calendar SLA engine, notification integration and activity retention policies.

## 6. Restore the fixtures

As Admin, open **Settings → Reset sample data**. This restores the original 30 tickets and seeded comments/events. No real customer information is touched.

## Interview topics

- REST resource design and HTTP status codes.
- Ticket lifecycle and ownership rules.
- Input validation and auditability.
- Demo role permissions vs actual authentication and why headers are insufficient for production.
- Why a local TSV backend was used as an executable reference, and how Spring Boot with JPA would replace it.
- Browser state management, filtering, responsive layout, and CSV export.
