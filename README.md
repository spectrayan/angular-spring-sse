# angular-spring-sse
A sample project to showcase how to implement Server-Sent Events (SSE) from a Spring Boot backend and an Angular frontend.

## Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+ and npm
- Git Bash (Windows) or any shell with GNU Make available
  - On Windows you can install Make via: `choco install make` (Chocolatey) or `winget install GnuWin32.Make`, or use Git Bash which already provides a POSIX shell.

## Quick start (Makefile)
This repository includes a `Makefile` that builds and runs the backend first and then the frontend.

Common targets:
- `make build` — builds the backend (Spring Boot) and the frontend (Angular) in sequence.
- `make run` — starts the backend first, waits for it to be up, then starts the Angular dev server.
- `make start` — alias for `make run`.
- `make clean` — cleans Maven artifacts and removes `dist`.

### What it does
- Backend
  - Location: `apps/backend/provider-service`
  - Port: `8080` (default Spring Boot)
- Frontend (Angular via Nx)
  - App name: `provider`
  - Dev server port: `4200`
  - A dev proxy is configured so that `/sse` calls are forwarded to `http://localhost:8080` (see `apps/provider/proxy.conf.json`).

### Usage
1. Install dependencies for the frontend if you haven’t yet:
   ```bash
   npm install
   ```
2. Build both projects:
   ```bash
   make build
   ```
3. Run both (backend first, then frontend):
   ```bash
   make run
   ```
   - Backend will start on `http://localhost:8080/`
   - Frontend dev server will start on `http://localhost:4200/`

Press Ctrl+C in the terminal to stop the frontend dev server. If the backend continues running, stop it from the same terminal session (it will often terminate when the shell exits) or kill the Java process manually.

## Running without Make (alternative)
If you prefer to run pieces manually or don’t have Make:

- Start backend (from repo root):
  ```bash
  mvn -f apps/backend/provider-service/pom.xml spring-boot:run
  ```
- In a second terminal, start the frontend dev server:
  ```bash
  npx nx serve provider
  ```

## Troubleshooting
- If `make` is not found on Windows, use Git Bash or install Make via Chocolatey/Winget as noted above. Alternatively, use the manual commands in the section above.
- If port 8080 is in use, change Spring Boot’s port (e.g., `--server.port=8081`) and update `apps/provider/proxy.conf.json` accordingly, or run `make` with a different port: `BACKEND_PORT=8081 make run`.
- If port 4200 is in use, run the Angular dev server with a different port: `npx nx serve provider --port=4300`.
