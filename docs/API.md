# ServiceDesk HTTP API

Base URL for the verified Java edition: `http://localhost:8080`.

## Bootstrap

`GET /api/bootstrap` returns the synthetic ticket collection, users, comments, activity entries, knowledge metadata and an ISO timestamp. All API responses are UTF-8 JSON except the CSV export and static resources.

## Create a ticket (PowerShell)

```powershell
$headers = @{ 'X-Demo-Role'='Requester'; 'X-Demo-User'='Ana Paredes' }
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/tickets' -Headers $headers -Body @{
  title = 'Workstation access problem'
  description = 'I cannot access the intranet from my work laptop after login.'
  category = 'Identity & Access'
  priority = 'High'
}
```

Sample success response (HTTP 201):

```json
{"id":2431,"title":"Workstation access problem","priority":"High","status":"Open","assignee":"Unassigned"}
```

The actual response contains additional fields and the ID depends on current local data.

## Change status

```powershell
$headers = @{ 'X-Demo-Role'='Technician'; 'X-Demo-User'='Lucía Ramos' }
Invoke-RestMethod -Method Patch -Uri 'http://localhost:8080/api/tickets/2401/status' -Headers $headers -Body @{ status='In progress' }
```

## Assign a technician

```powershell
$headers = @{ 'X-Demo-Role'='Admin'; 'X-Demo-User'='Diego Flores' }
Invoke-RestMethod -Method Patch -Uri 'http://localhost:8080/api/tickets/2401/assign' -Headers $headers -Body @{ assignee='Marco Vidal' }
```

## Add a comment

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/tickets/2401/comments' -Headers $headers -Body @{ message='Investigating VPN configuration.' }
```

## Export a report

Open `/api/export` in your browser or download the CSV using `Invoke-WebRequest -OutFile tickets.csv`.

## Role behavior

- Requester: create tickets, read demo workspace and comment.
- Technician: requester actions plus status transitions; cannot re-open a closed ticket.
- Admin: technician actions plus assigning tickets and restoring demo data.

**Security notice:** `X-Demo-Role` and `X-Demo-User` are supplied by the client and are only suitable for educational simulation. Real role-based security requires server-side identity, password/session management and non-forgeable authorization.

## Errors

- `400` invalid request fields (title, description, priority, status, assignee, comment).
- `403` role not permitted.
- `404` ticket or route not found.
- `500` unexpected server error (details logged locally, not disclosed in the API response).
