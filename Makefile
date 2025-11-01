# Makefile for building and running backend (Spring Boot) first, then frontend (Angular via Nx)
# Prerequisites:
# - Java 17+
# - Maven 3.9+
# - Node.js 18+ and npm
# - Nx (npx will be used automatically, no global install required)

SHELL := /bin/sh

# Paths and ports
BACKEND_DIR := apps/backend/provider-service
BACKEND_POM := $(BACKEND_DIR)/pom.xml
BACKEND_PORT ?= 8080
FRONTEND_APP := provider
FRONTEND_PORT ?= 4200

# Helper: wait until a TCP port is open using Node.js (portable across shells)
# Usage: $(MAKE) wait-port PORT=8080
.PHONY: wait-port
wait-port:
	@echo "Waiting for port $(PORT) to be open..."
	@node -e "const n=require('net'); const p=parseInt(process.env.PORT||'0',10); if(!p){console.error('PORT env var required'); process.exit(2);} (function t(){const s=n.createConnection({port:p},()=>{console.log('Port ' + p + ' is open.'); s.end(); process.exit(0);}); s.on('error',()=>setTimeout(t,500));})();" || (echo "Node is required to wait for port." && exit 1)

.PHONY: backend-build
backend-build:
	@mvn -f $(BACKEND_POM) -q -DskipTests clean package
	@echo "Backend build complete."

.PHONY: backend-run
backend-run:
	@echo "Starting backend (Spring Boot) on port $(BACKEND_PORT) ..."
	@mvn -f $(BACKEND_POM) spring-boot:run

.PHONY: frontend-build
frontend-build:
	@npx --yes nx build $(FRONTEND_APP)
	@echo "Frontend build complete. Output: dist/apps/$(FRONTEND_APP)"

.PHONY: frontend-serve
frontend-serve:
	@echo "Starting frontend (Nx dev-server) on port $(FRONTEND_PORT) ..."
	@npx --yes nx serve $(FRONTEND_APP)

.PHONY: build
build: backend-build frontend-build
	@echo "Build finished for backend and frontend."

# Run both apps: backend first (in background), wait until it's up, then frontend
# Stop with Ctrl+C in the terminal where 'run' was started. This will stop the frontend;
# backend started in background within the same shell will terminate when the shell exits.
.PHONY: run
run:
	@echo "Launching backend in background..."
	@$(MAKE) backend-run & \
	  BACK_PID=$$!; \
	  echo "Backend PID: $$BACK_PID"; \
	  PORT=$(BACKEND_PORT) $(MAKE) wait-port; \
	  echo "Launching frontend now..."; \
	  npx --yes nx serve $(FRONTEND_APP); \
	  echo "Shutting down. If backend is still running, terminate it manually (PID $$BACK_PID)."

# Convenience aliases
.PHONY: start
start: run

.PHONY: clean
clean:
	@mvn -f $(BACKEND_POM) -q clean || true
	@node -e "const fs=require('fs'); try{fs.rmSync('dist',{recursive:true,force:true});}catch(e){} console.log('Removed dist');" || true
	@echo "Clean complete."
