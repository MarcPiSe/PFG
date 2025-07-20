# File Platform — OSS Project

This repository contains the full source code (Spring Boot backend + React/Vite frontend + Tauri client) for a self-hosted file manager.

## Project Status
- MVP is functional but pending final optimizations and security audits.
- **Not recommended for production use** without following the secure deployment guide.

## License
Distributed under the MIT License (see `LICENSE`). The software is provided **as is**, without warranty of any kind.

## Regulatory Compliance
1. Basic legal texts are available in `legal/`.
2. GDPR/ENS templates and checklists are in `docs/`.
3. The final deployment is the responsibility of the operator. See `docs/compliance.md`.

> **Important:** If real personal data is used, it is essential to customize `legal/PRIVACY.md`, complete the activity log, and enable HTTPS.

## Structure
```
server/        ← Spring Boot microservices
ui-new/        ← React + Vite + Tailwind web client
desktop/       ← Tauri desktop client
legal/         ← Legal notice and privacy policy
docs/          ← Compliance, GDPR templates
```

## Quick Start
1.  Run `docker compose -f server/compose.yml up -d` (starts backend + db + gateway + ui).
2.  Visit `http://localhost:8080` in your browser.

## Development

This project is configured to facilitate local development and debugging, especially with VSCode.

### Local Infrastructure with `test.yaml`

For local development, you don't need to run the entire microservices stack. The `server/test.yaml` file is a stripped-down Docker Compose configuration that only starts the essential infrastructure: **PostgreSQL** and **RabbitMQ**.

In case of necessity the system is configured to be able to execute part of the server and/or clients with the `server/test.yaml` (uncomment the services you are not changing) and have the rest in mode debug to save resources.

To start it, run:
`docker compose -f server/test.yaml up -d`

This allows you to run individual microservices directly from your IDE (like IntelliJ or VSCode with the Java Extension Pack) in debug mode, connecting them to the shared infrastructure provided by the containers.

### VSCode Debugging Setup

The repository includes a pre-configured VSCode debugging environment in the `.vscode/` directory:

-   **`launch.json`**: Contains debug configurations for each microservice. You can start a debugging session for any service (e.g., `Debug FileManagement`) directly from the "Run and Debug" panel in VSCode.
-   **`tasks.json`**: Defines helper tasks. For instance, the `wait-for-eureka` task ensures that services that depend on Eureka don't start until Eureka is ready, preventing startup errors during debugging.
-   **`settings.json`**: Contains workspace-specific settings to ensure a consistent development experience.

With this setup, you can easily run the entire backend in debug mode, set breakpoints, and inspect the code's behavior.

## Testing

### Manual Testing Checklist

A comprehensive manual testing checklist is available in the `tests/` directory. This document provides a systematic rundown of all system functionalities to verify correct operation after deployment or during development.

### Automated Testing (Work in Progress)

Unit testing and automated integration tests are currently being developed and will be added to the project in future iterations. The manual testing checklist serves as the primary validation method for now.

## Pre-production Checklist
Make sure to check all the boxes in `docs/compliance.md`.

## Credits and Source
Inspired by official templates from AEPD and INCIBE. Contributions are welcome via pull-request. 