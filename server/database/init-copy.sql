-- =================================================================
-- Table Creation
-- =================================================================

CREATE TABLE IF NOT EXISTS user_entity (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    last_password_change TIMESTAMP NOT NULL,
    role SMALLINT NOT NULL  -- Role as ordinal: 0=USER, 1=ADMIN, 2=SUPER_ADMIN
);


CREATE TABLE IF NOT EXISTS user_info (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS element_entity (
    id UUID PRIMARY KEY,
    is_folder BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS folder_entity (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id UUID,
    parent_id UUID,
    element_id_id UUID,
    creation_date TIMESTAMP,
    last_modification TIMESTAMP,
    shared BOOLEAN,
    deleted BOOLEAN,
    FOREIGN KEY (parent_id) REFERENCES folder_entity(id),
    FOREIGN KEY (element_id_id) REFERENCES element_entity(id)
);

CREATE TABLE IF NOT EXISTS file_entity (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size BIGINT,
    user_id UUID,
    parent_id UUID,
    element_id_id UUID,
    creation_date TIMESTAMP,
    last_modification TIMESTAMP,
    shared BOOLEAN,
    deleted BOOLEAN,
    mime_type VARCHAR(255),
    FOREIGN KEY (parent_id) REFERENCES folder_entity(id),
    FOREIGN KEY (element_id_id) REFERENCES element_entity(id)
);

CREATE TABLE IF NOT EXISTS access_rule (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    access_type VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS shared_access (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    root BOOLEAN,
    shared_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trash_record (
    id UUID PRIMARY KEY,
    user_id UUID,
    element_id UUID,
    sharing BOOLEAN,
    access BOOLEAN,
    manager BOOLEAN,
    root BOOLEAN,
    deletion_date TIMESTAMP,
    expiration_date TIMESTAMP,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS user_deletion_process (
    user_id UUID PRIMARY KEY,
    file_management_status VARCHAR(255),
    file_sharing_status VARCHAR(255),
    file_access_control_status VARCHAR(255),
    user_management_status VARCHAR(255),
    user_authentication_status VARCHAR(255),
    trash_status VARCHAR(255),
    sync_service_status VARCHAR(255),
    created_at TIMESTAMP
);

-- =================================================================
-- SyncService Tables
-- =================================================================

CREATE TABLE IF NOT EXISTS snapshot_entity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS snapshot_element_entity (
    id UUID PRIMARY KEY,
    element_id UUID NOT NULL,
    snapshot_id UUID,
    parent_id UUID,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    hash VARCHAR(64) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    FOREIGN KEY (snapshot_id) REFERENCES snapshot_entity(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES snapshot_element_entity(id) ON DELETE CASCADE
);



-- =================================================================
-- Mock Data for TFG Project - v3 (Corrected FileManagement Schema)
-- =================================================================

/*
Estructura de datos por usuario:

Usuario1 (11111111-1111-1111-1111-111111111111):
- Carpeta raíz (a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1)
  └── Carpeta "Documents" (a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2)
      ├── Archivo "report.txt" (a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3)
      └── Archivo "old_document.txt" (a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4)
Comparte:
- "Documents" con usuario2 (READ)
- "report.txt" con usuario2 (READ)
- Carpeta raíz con usuario3 (READ)
- "old_document.txt" con usuario4 (WRITE)
Recibe:
- Carpeta raíz de usuario2 (READ)
- "design_document.pdf" de usuario3 (WRITE)
- Carpeta raíz de usuario4 (WRITE)

Usuario2 (22222222-2222-2222-2222-222222222222):
- Carpeta raíz (a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5)
Comparte:
- Carpeta raíz con usuario1 (READ)
Recibe:
- "Documents" de usuario1 (READ)
- "report.txt" de usuario1 (READ)

Usuario3 (33333333-3333-3333-3333-333333333333):
- Carpeta raíz (d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1)
  └── Archivo "design_document.pdf" (d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2)
Comparte:
- "design_document.pdf" con usuario1 (WRITE)
Recibe:
- Carpeta raíz de usuario1 (READ)

Usuario4 (44444444-4444-4444-4444-444444444444):
- Carpeta raíz (a6a6a6a6-a6a6-a6a6-a6a6-a6a6a6a6a6a6)
  └── Carpeta "Proyectos" (a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7)
      ├── Archivo "presentacion.pptx" (a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8)
      ├── Archivo "memoria.pdf" (a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9)
      └── Archivo "codigo.zip" (b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0)
Comparte:
- Carpeta "Proyectos" con usuario1 (WRITE)
Recibe:
- "old_document.txt" de usuario1 (WRITE)
*/

-- User Credentials (username: password)
-- user1: ComplexPassword123!
-- user2: ComplexPassword123!
-- user3: ComplexPassword123!
-- user4: ComplexPassword123!

-- ---------------------------------
-- USERS
-- ---------------------------------
INSERT INTO user_entity (id, username, password, last_password_change, role) VALUES
('11111111-1111-1111-1111-111111111111', 'user1', '$2a$10$hN5u7oRePfRw6FnvxydY4eG16fVC0TqdBDNY1.wtvSG9ZEsGrsV92', NOW(), '2');

INSERT INTO user_info (id, email, first_name, last_name, created_date, last_modified_date) VALUES
('11111111-1111-1111-1111-111111111111', 'user1@example.com', 'Test', 'User1', NOW(), NOW());

-- ---------------------------------
-- FILE STRUCTURE
-- ---------------------------------

-- === User 1 Structure ===
-- Element Pointers for user1
INSERT INTO element_entity (id, is_folder) VALUES
('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', true);
-- Folders for user1
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES
('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1', 'root', '11111111-1111-1111-1111-111111111111', null, 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', NOW(), NOW(), false, false);

-- Añadir reglas de acceso ADMIN para cada usuario en sus propios elementos
-- Usuario 1 (ADMIN access para sus elementos)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ad111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'ADMIN');

-- =================================================================
-- SNAPSHOT DATA (SyncService Mock Data)
-- =================================================================

/*
Los snapshots representan el estado actual de los archivos de cada usuario.
Se calculan los hashes según el algoritmo del SnapshotService:
- Files: Hash simulado (SHA-256)
- Folders: Hash basado en hashes de contenido ordenado alfabéticamente

Estructura de snapshots:
Usuario1: root -> Documents -> [old_document.txt, report.txt]
Usuario2: root (vacía)
Usuario3: root -> design_document.pdf
Usuario4: root -> Proyectos -> [codigo.zip, memoria.pdf, presentacion.pptx]
*/

-- ---------------------------------
-- SNAPSHOTS - Crear snapshots base para cada usuario
-- ---------------------------------

INSERT INTO snapshot_entity (id, user_id) VALUES
('10000001-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111'); 

-- ---------------------------------
-- SNAPSHOT ELEMENTS - Usuario1
-- ---------------------------------

-- 1. Usuario1 - root (carpeta raíz) - PRIMERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('1e111111-1111-1111-1111-111111111111', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', '10000001-1111-1111-1111-111111111111', null, 'folder', '/', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '/');

