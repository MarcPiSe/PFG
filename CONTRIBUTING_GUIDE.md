# Contributing Guide

First off, thank you for considering contributing to this project! Your help is greatly appreciated. This document provides a technical overview to help you get started with the development.

## Table of Contents

- [Contributing Guide](#contributing-guide)
  - [Table of Contents](#table-of-contents)
  - [Architectural Overview](#architectural-overview)
  - [Backend Microservices](#backend-microservices)
    - [Gateway \& JWT Filter](#gateway--jwt-filter)
    - [User Management \& Authentication](#user-management--authentication)
    - [File Management](#file-management)
    - [File Sharing](#file-sharing)
    - [Trash Service](#trash-service)
    - [Sync Service](#sync-service)
  - [Web Client (React)](#web-client-react)
    - [Key Components \& Hooks](#key-components--hooks)
    - [State Management](#state-management)
    - [API Communication](#api-communication)
  - [Desktop Client (Tauri + Svelte)](#desktop-client-tauri--svelte)
    - [Window Management](#window-management)
    - [Synchronization Engine](#synchronization-engine)

## Architectural Overview

The system is built on a **microservices architecture**. The backend consists of several independent services, each containerized with Docker and orchestrated by a `compose.yml` file. This design ensures modularity, scalability, and maintainability.

Communication follows two main patterns:
1.  **Synchronous HTTP requests** for user-facing operations (login, file upload) via a central **API Gateway**.
2.  **Asynchronous messaging with RabbitMQ** for background processes (e.g., cascading deletes), enhancing resilience.
3.  **Persistent WebSocket connections** for real-time synchronization between clients.

## Backend Microservices

The backend is composed of several Spring Boot services. Each follows a layered architecture (controllers, services, repositories).

### Gateway & JWT Filter

-   **Service:** `Gateway`
-   **Description:** The single entry point for all external requests. It centralizes routing and security.
-   **Key Component:** `JwtAuthenticationFilter`. This filter intercepts protected requests, validates the JWT token by calling the `UserAuthentication` service, and enriches the request with `X-User-Id` and `X-Connection-Id` headers. It also handles role-based access for admin routes.

### User Management & Authentication

-   **Services:** `UserAuthentication`, `UserManagement`
-   **Description:** These services work together as an aggregate.
    -   `UserAuthentication` is the aggregate root, managing critical security data (credentials, tokens). It handles user registration, login, and token generation/validation.
    -   `UserManagement` handles user profile data (email, name, etc.). It also orchestrates the complex, resilient user deletion process.
-   **Key Process (User Deletion):** When a user account is deleted, `UserManagement` initiates a two-phase process:
    1.  **Synchronous:** Immediately calls `UserAuthentication` to delete credentials, blocking further logins.
    2.  **Asynchronous:** Publishes messages to RabbitMQ to instruct all other services to purge user-related data. A state machine (`UserDeletionProcess` entity) tracks the progress, and a scheduled task retries any pending deletions to ensure full data removal.

### File Management

-   **Service:** `FileManagement`
-   **Description:** The core service for all file and folder operations (CRUD).
-   **Key Entities:** `FileEntity`, `FolderEntity`, and a shared `ElementEntity`. The `ElementEntity` provides a common `elementId` and an `isFolder` flag, allowing for polymorphic handling of elements.
-   **Storage Strategy:** Files are not stored with their original names on disk. Instead, the internal UUID of the `FileEntity` is used as the filename. This prevents collisions and adds a layer of security through obfuscation.
-   **Uploads:** It exposes two endpoints for uploads: a standard `multipart/form-data` endpoint for the web client and a streaming `application/octet-stream` endpoint for the desktop client.

### File Sharing

-   **Service:** `FileSharing`
-   **Description:** Manages the logic for sharing files and folders between users.
-   **Key Logic:** It maintains `SharedAccess` records. When an element is shared, it creates an access rule in `FileAccessControl` and a `SharedAccess` record. A `root` flag on this record is crucial for the UI to efficiently display the "Shared with me" view by only showing top-level shared items.

### Trash Service

-   **Service:** `TrashService`
-   **Description:** Implements the recycle bin functionality.
-   **Key Process (Deletion):**
    1.  **Soft Delete:** Moving an item to the trash is a logical operation. The service calls `FileManagement` to mark the item as `deleted` and creates a `TrashRecord` with an expiration date.
    2.  **Hard Delete:** Permanent deletion is an orchestrated, asynchronous process. It first synchronously calls `FileManagement` to delete the physical file, then uses RabbitMQ with a confirmation and retry mechanism (similar to user deletion) to ensure all related data in other services (`FileAccessControl`, `FileSharing`) is purged.

### Sync Service

-   **Service:** `SyncService`
-   **Description:** Manages real-time synchronization via WebSockets.
-   **Key Components:**
    -   **WebSocket Handler:** Manages client connections. An interceptor (`UserIdHandshakeInterceptor`) extracts user and connection IDs from headers.
    -   **RabbitMQ Consumer:** Listens for change events from other services.
    -   **Snapshot Service:** Maintains a database representation of each user's file tree (a "snapshot").
-   **Key Logic (Hash-based Sync):** When a change occurs, the `SnapshotService` updates the user's file tree and recursively recalculates the hash of all parent folders up to the root. This allows clients (especially the desktop client) to detect any change by simply comparing the root hash.
-   **Diffusion Strategy:** It uses a dual strategy:
    -   **Web Client:** Receives a simple `updated_tree` command, prompting it to refetch data for the affected folder.
    -   **Desktop Client:** Receives the entire updated snapshot, replacing its local state to ensure full consistency.

## Web Client (React)

The web client is a Single Page Application built with React, Vite, TypeScript, and Tailwind CSS.

### Key Components & Hooks

-   **`pages/FileManager`**: The main file management interface.
-   **`hooks/useFileOperations.ts`**: The core of the business logic. It uses TanStack Query's `useMutation` for all write operations (create, move, delete) and implements **optimistic updates** for a snappy UI.
-   **`hooks/useAuth.tsx`**: Manages user authentication state and logic.
-   **`lib/api.ts`**: Configures Axios, including an interceptor for automatic token renewal using the refresh token.
-   **`lib/websocket.ts`**: Manages the WebSocket connection for real-time updates.

### State Management

The frontend state is primarily managed by a combination of Zustand for global UI state and TanStack React Query for server state. State that needs to persist across page reloads is saved to `sessionStorage` via a `safeSessionStorage` wrapper.

-   **Zustand:** Used for synchronous, client-side state.
    -   **`fileStore`**: The core data store. It holds the complete `folderStructure` (for the sidebar), the `currentDirectory`'s contents, and which folders are `expandedFolders`. It's responsible for fetching the initial state from the server.
    -   **`fileSelectionStore`**: Manages UI interactions related to file selection. This includes the `selectedFiles` list, handling complex multi-select logic (Ctrl/Shift keys), and managing the `clipboard` for cut/copy/paste operations.
    -   **`fileContextStore`**: Manages the global context of the file manager view. It tracks the current `section` (`root`, `trash`, or `shared`) and UI settings like filters and sorting preferences.

-   **TanStack React Query:** Manages all asynchronous operations related to server data. It handles fetching, caching, and invalidation of data, and is key to implementing optimistic updates for operations like moving or renaming files, providing a responsive and seamless user experience.

### API Communication

All communication with the backend is abstracted through services defined in `lib/api.ts`. This file sets up an Axios instance with a base URL and an interceptor that handles JWT token management and automatic renewal.

## Desktop Client (Tauri + Svelte)

The desktop client is designed as a background service for continuous file synchronization.

### Window Management

-   **Framework:** Tauri with a Svelte 5 frontend.
-   **Core Logic:** The Rust backend (`src-tauri`) manages window presentation. Based on the `config.json` file, it decides whether to show the initial configuration window, the login window, or the main transfer monitor window.
-   **Communication:** The Svelte UI communicates with the Rust backend via Tauri's `invoke` API, which calls Rust functions exposed as commands.

### Synchronization Engine

-   **Module:** `synchronizer` in Rust.
-   **Architecture:** It runs two main asynchronous tasks:
    1.  A **WebSocket client** to listen for remote changes from the server.
    2.  A **local file system watcher** (using the `notify` crate) to detect local changes.
-   **Core Logic (Tree-based Diffing):**
    -   The client maintains a local representation of the file tree in a `tree.json` file, including hashes for each file and folder.
    -   When a change is detected (either locally or from a remote snapshot), the function `fstree::diff_trees` performs a recursive comparison between the old and new trees.
    -   It generates a list of changes (Added, Deleted, Modified). A subsequent step, `fstree::detect_renames`, cleverly identifies renames by matching file hashes between "deleted" and "added" entries, optimizing the operation.
    -   Based on the final list of changes, the client performs the necessary actions (upload, download, delete, rename).
-   **Concurrency:** To improve performance, especially when syncing multiple files, the engine gathers all required download/upload operations into a list of `futures` and executes them concurrently using `join_all`. 