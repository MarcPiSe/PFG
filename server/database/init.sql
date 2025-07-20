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
('11111111-1111-1111-1111-111111111111', 'user1', '$2a$10$hN5u7oRePfRw6FnvxydY4eG16fVC0TqdBDNY1.wtvSG9ZEsGrsV92', NOW(), '2'),
('22222222-2222-2222-2222-222222222222', 'user2', '$2a$10$hN5u7oRePfRw6FnvxydY4eG16fVC0TqdBDNY1.wtvSG9ZEsGrsV92', NOW(), '0'),
('33333333-3333-3333-3333-333333333333', 'user3', '$2a$10$hN5u7oRePfRw6FnvxydY4eG16fVC0TqdBDNY1.wtvSG9ZEsGrsV92', NOW(), '0'),
('44444444-4444-4444-4444-444444444444', 'user4', '$2a$10$hN5u7oRePfRw6FnvxydY4eG16fVC0TqdBDNY1.wtvSG9ZEsGrsV92', NOW(), '1');

INSERT INTO user_info (id, email, first_name, last_name, created_date, last_modified_date) VALUES
('11111111-1111-1111-1111-111111111111', 'user1@example.com', 'Test', 'User1', NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'user2@example.com', 'Test', 'User2', NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'user3@example.com', 'Test', 'User3', NOW(), NOW()),
('44444444-4444-4444-4444-444444444444', 'user4@example.com', 'Test', 'User4', NOW(), NOW());

-- ---------------------------------
-- FILE STRUCTURE
-- ---------------------------------

-- === User 1 Structure ===
-- Element Pointers for user1
INSERT INTO element_entity (id, is_folder) VALUES
('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', true),
('a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', true),
('a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', false),
('a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', false);
-- Folders for user1
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES
('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1', 'root', '11111111-1111-1111-1111-111111111111', null, 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', NOW(), NOW(), false, false),
('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'Documents', '11111111-1111-1111-1111-111111111111', 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', NOW(), NOW(), true, false);
-- Files for user1
INSERT INTO file_entity (id, name, size, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted, mime_type) VALUES
('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'report.txt', 25, '11111111-1111-1111-1111-111111111111', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', NOW(), NOW(), true, false, 'text/plain'),
('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'old_document.txt', 50, '11111111-1111-1111-1111-111111111111', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', NOW(), NOW(), false, true, 'text/plain');

-- === User 2 Structure ===
-- Element Pointers for user2
INSERT INTO element_entity (id, is_folder) VALUES
('a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', true);
-- Folders for user1
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES
('b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3', 'root', '22222222-2222-2222-2222-222222222222', null, 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', NOW(), NOW(), false, false);


-- === User 3 Structure ===
-- Element Pointers for user3
INSERT INTO element_entity (id, is_folder) VALUES
('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', true),
('d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', false);
-- Folders for user3
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES
('e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1', 'root', '33333333-3333-3333-3333-333333333333', null, 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', NOW(), NOW(), false, false);
-- Files for user3
INSERT INTO file_entity (id, name, size, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted, mime_type) VALUES
('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 'design_document.pdf', 100, '33333333-3333-3333-3333-333333333333', 'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', NOW(), NOW(), true, false, 'application/pdf');

-- === User 4 Structure ===
-- Element Pointers for user4
INSERT INTO element_entity (id, is_folder) VALUES
('a6a6a6a6-a6a6-a6a6-a6a6-a6a6a6a6a6a6', true),
('a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', true),
('a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', false),
('a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', false),
('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', false);

-- Folders for user4
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES
('b4b4b4b4-b4b4-b4b4-b4b4-b4b4b4b4b4b4', 'root', '44444444-4444-4444-4444-444444444444', null, 'a6a6a6a6-a6a6-a6a6-a6a6-a6a6a6a6a6a6', NOW(), NOW(), false, false),
('b5b5b5b5-b5b5-b5b5-b5b5-b5b5b5b5b5b5', 'Proyectos', '44444444-4444-4444-4444-444444444444', 'b4b4b4b4-b4b4-b4b4-b4b4-b4b4b4b4b4b4', 'a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', NOW(), NOW(), true, false);

-- Files for user4
INSERT INTO file_entity (id, name, size, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted, mime_type) VALUES
('c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'presentacion.pptx', 1024, '44444444-4444-4444-4444-444444444444', 'b5b5b5b5-b5b5-b5b5-b5b5-b5b5b5b5b5b5', 'a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', NOW(), NOW(), true, false, 'application/vnd.openxmlformats-officedocument.presentationml.presentation'),
('c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', 'memoria.pdf', 2048, '44444444-4444-4444-4444-444444444444', 'b5b5b5b5-b5b5-b5b5-b5b5-b5b5b5b5b5b5', 'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', NOW(), NOW(), true, false, 'application/pdf'),
('c6c6c6c6-c6c6-c6c6-c6c6-c6c6c6c6c6c6', 'codigo.zip', 5120, '44444444-4444-4444-4444-444444444444', 'b5b5b5b5-b5b5-b5b5-b5b5-b5b5b5b5b5b5', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', NOW(), NOW(), true, false, 'application/zip');

-- Añadir reglas de acceso ADMIN para cada usuario en sus propios elementos
-- Usuario 1 (ADMIN access para sus elementos)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ad111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'ADMIN'),
('ad111111-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', 'ADMIN'),
('ad111111-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', 'ADMIN'),
('ad111111-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', 'ADMIN');

-- Usuario 2 (ADMIN access para sus elementos)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ad222222-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', 'ADMIN');

-- Usuario 3 (ADMIN access para sus elementos)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ad333333-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', 'ADMIN'),
('ad333333-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', 'ADMIN');

-- Usuario 4 (ADMIN access para sus elementos)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ad444444-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 'a6a6a6a6-a6a6-a6a6-a6a6-a6a6a6a6a6a6', 'ADMIN'),
('ad444444-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', 'ADMIN'),
('ad444444-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', 'a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', 'ADMIN'),
('ad444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', 'ADMIN'),
('ad444444-5555-5555-5555-555555555555', '44444444-4444-4444-4444-444444444444', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'ADMIN');


-- ---------------------------------
-- ACCESS, SHARING & TRASH
-- ---------------------------------

-- Share user1's "Documents" folder with user2 (READ access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('a5b6c7d8-e9f0-a1b2-c3d4-e5f6a7b8c9d0', '22222222-2222-2222-2222-222222222222', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', 'READ');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('b5c6d7e8-f9a0-b1c2-d3e4-f5a6b7c8d9e0', '22222222-2222-2222-2222-222222222222', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', true);

-- Inherit READ access for children of "Documents" folder for user2
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('00000001-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', 'READ'), -- report.txt
('00000002-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', 'READ'); -- old_document.txt

-- Share user1's root folder with user3 (READ access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('c5d6e7f8-a9b0-c1d2-e3f4-a5b6c7d8e9f0', '33333333-3333-3333-3333-333333333333', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'READ');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('d5e6f7a8-b9c0-d1e2-f3a4-b5c6d7e8f9a0', '33333333-3333-3333-3333-333333333333', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', true);

-- Inherit READ access for children of root folder for user3
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('00000003-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', 'READ'), -- Documents folder
('00000004-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', 'READ'), -- report.txt
('00000005-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', 'READ'); -- old_document.txt

-- Share user1's old_document.txt with user4 (WRITE access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('e5f6a7b8-c9d0-e1f2-a3b4-c5d6e7f8a9b0', '44444444-4444-4444-4444-444444444444', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', 'WRITE');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('f5a6b7c8-d9e0-f1a2-b3c4-d5e6f7a8b9c0', '44444444-4444-4444-4444-444444444444', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', true);

-- Share user2's root folder with user1 (READ access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('aabbccdd-1234-5678-9abc-def012345678', '11111111-1111-1111-1111-111111111111', 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', 'READ');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('bbccddee-2345-6789-abcd-ef0123456789', '11111111-1111-1111-1111-111111111111', 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', true);

-- Share user3's design_document.pdf with user1 (WRITE access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('ccddeeff-3456-789a-bcde-f0123456789a', '11111111-1111-1111-1111-111111111111', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', 'WRITE');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('ddeeffaa-4567-89ab-cdef-0123456789ab', '11111111-1111-1111-1111-111111111111', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', true);

-- Share user4's "Proyectos" folder with user1 (WRITE access)
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('eeffaabb-5678-9abc-def0-123456789abc', '11111111-1111-1111-1111-111111111111', 'a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', 'WRITE');
INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('ffaabbcc-6789-abcd-ef01-23456789abcd', '11111111-1111-1111-1111-111111111111', 'a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', true);

-- Inherit WRITE access for children of "Proyectos" folder for user1
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES
('00000006-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', 'WRITE'), -- presentacion.pptx
('00000007-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', 'WRITE'), -- memoria.pdf
('00000008-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'WRITE'); -- codigo.zip

INSERT INTO shared_access (id, user_id, element_id, root) VALUES
('ffaabbcc-6789-abcd-ef01-111111111111', '11111111-1111-1111-1111-111111111111', 'a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', false),
('ffaabbcc-6789-abcd-ef01-111111111211', '11111111-1111-1111-1111-111111111111', 'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', false),
('ffaabbcc-6789-abcd-ef01-111111111311', '11111111-1111-1111-1111-111111111111', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', false);

-- Record for user1's trashed file
INSERT INTO trash_record (id, user_id, element_id, sharing, access, manager, root, deletion_date, expiration_date, status) VALUES
('e1f2a3b4-c5d6-e7f8-a9b0-c1d2e3f4a5b6', '11111111-1111-1111-1111-111111111111', 'a4a4a4a4-a4a4-a4a4-a4a4-a4a4a4a4a4a4', false, false, false, true, NOW(), NOW() + INTERVAL '30 day', 'ACTIVE');

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
('10000001-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111'), -- Usuario1
('20000002-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222'), -- Usuario2
('30000003-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333'), -- Usuario3
('40000004-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444'); -- Usuario4

-- ---------------------------------
-- SNAPSHOT ELEMENTS - Usuario1
-- ---------------------------------

-- 1. Usuario1 - root (carpeta raíz) - PRIMERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('1e111111-1111-1111-1111-111111111111', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', '10000001-1111-1111-1111-111111111111', null, 'folder', '/', 'a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6', '/');

-- 2. Usuario1 - Documents (carpeta) - SEGUNDO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('1e111111-2222-2222-2222-222222222222', 'a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2', '10000001-1111-1111-1111-111111111111', '1e111111-1111-1111-1111-111111111111', 'folder', 'Documents', 'e4e6b8b2c6c7c6e8f9a1b3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4', '/Documents');

-- 3. Usuario1 - report.txt (archivo) - TERCERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('1e111111-3333-3333-3333-333333333333', 'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a3', '10000001-1111-1111-1111-111111111111', '1e111111-2222-2222-2222-222222222222', 'file', 'report.txt', 'f1d2d2f924e986ac86fdf7b36c94bcdf32beec15f6a7b8c9d0e1f2a3b4c5d6e7', '/Documents/report.txt');

-- ---------------------------------
-- SNAPSHOT ELEMENTS - Usuario2
-- ---------------------------------

-- Usuario2 - root (carpeta raíz vacía)
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('2e222222-1111-1111-1111-111111111111', 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a5a5', '20000002-2222-2222-2222-222222222222', null, 'folder', '/', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '/');

-- ---------------------------------
-- SNAPSHOT ELEMENTS - Usuario3
-- ---------------------------------

-- 1. Usuario3 - root (carpeta raíz) - PRIMERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('3e333333-1111-1111-1111-111111111111', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1', '30000003-3333-3333-3333-333333333333', null, 'folder', '/', 'd8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9', '/');

-- 2. Usuario3 - design_document.pdf (archivo) - SEGUNDO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('3e333333-2222-2222-2222-222222222222', 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d2d2', '30000003-3333-3333-3333-333333333333', '3e333333-1111-1111-1111-111111111111', 'file', 'design_document.pdf', 'c4b73e8b8c5b8b3e1d4e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9', '/design_document.pdf');

-- ---------------------------------
-- SNAPSHOT ELEMENTS - Usuario4
-- ---------------------------------

-- 1. Usuario4 - root (carpeta raíz) - PRIMERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('4e444444-1111-1111-1111-111111111111', 'a6a6a6a6-a6a6-a6a6-a6a6-a6a6a6a6a6a6', '40000004-4444-4444-4444-444444444444', null, 'folder', '/', 'c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8', '/');

-- 2. Usuario4 - Proyectos (carpeta) - SEGUNDO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('4e444444-2222-2222-2222-222222222222', 'a7a7a7a7-a7a7-a7a7-a7a7-a7a7a7a7a7a7', '40000004-4444-4444-4444-444444444444', '4e444444-1111-1111-1111-111111111111', 'folder', 'Proyectos', 'f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7', '/Proyectos');

-- 3. Usuario4 - codigo.zip (archivo) - TERCERO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('4e444444-5555-5555-5555-555555555555', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '40000004-4444-4444-4444-444444444444', '4e444444-2222-2222-2222-222222222222', 'file', 'codigo.zip', '1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2', '/Proyectos/codigo.zip');

-- 4. Usuario4 - memoria.pdf (archivo) - CUARTO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('4e444444-4444-4444-4444-444444444444', 'a9a9a9a9-a9a9-a9a9-a9a9-a9a9a9a9a9a9', '40000004-4444-4444-4444-444444444444', '4e444444-2222-2222-2222-222222222222', 'file', 'memoria.pdf', '2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3', '/Proyectos/memoria.pdf');

-- 5. Usuario4 - presentacion.pptx (archivo) - QUINTO
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES
('4e444444-3333-3333-3333-333333333333', 'a8a8a8a8-a8a8-a8a8-a8a8-a8a8a8a8a8a8', '40000004-4444-4444-4444-444444444444', '4e444444-2222-2222-2222-222222222222', 'file', 'presentacion.pptx', '3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4', '/Proyectos/presentacion.pptx');

-- Note: Los hashes de folders se calculan según el algoritmo del SnapshotService:
-- SHA-256 de la concatenación de hashes de contenido ordenado alfabéticamente
-- Los hashes de archivos son simulados pero realistas (SHA-256 format)
