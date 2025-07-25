--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5 (Debian 17.5-1.pgdg120+1)
-- Dumped by pg_dump version 17.5 (Debian 17.5-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: access_rule; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.access_rule (
    id uuid NOT NULL,
    user_id uuid,
    element_id uuid,
    access_type character varying(255)
);


ALTER TABLE public.access_rule OWNER TO admin;

--
-- Name: element_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.element_entity (
    id uuid NOT NULL,
    is_folder boolean NOT NULL
);


ALTER TABLE public.element_entity OWNER TO admin;

--
-- Name: file_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.file_entity (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    size bigint,
    user_id uuid,
    parent_id uuid,
    element_id_id uuid,
    creation_date timestamp without time zone,
    last_modification timestamp without time zone,
    shared boolean,
    deleted boolean,
    mime_type character varying(255)
);


ALTER TABLE public.file_entity OWNER TO admin;

--
-- Name: folder_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.folder_entity (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    user_id uuid,
    parent_id uuid,
    element_id_id uuid,
    creation_date timestamp without time zone,
    last_modification timestamp without time zone,
    shared boolean,
    deleted boolean
);


ALTER TABLE public.folder_entity OWNER TO admin;

--
-- Name: shared_access; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.shared_access (
    id uuid NOT NULL,
    user_id uuid,
    element_id uuid,
    root boolean,
    shared_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.shared_access OWNER TO admin;

--
-- Name: snapshot_element_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.snapshot_element_entity (
    id uuid NOT NULL,
    element_id uuid NOT NULL,
    snapshot_id uuid,
    parent_id uuid,
    type character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    hash character varying(64) NOT NULL,
    path character varying(1000) NOT NULL
);


ALTER TABLE public.snapshot_element_entity OWNER TO admin;

--
-- Name: snapshot_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.snapshot_entity (
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.snapshot_entity OWNER TO admin;

--
-- Name: trash_record; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.trash_record (
    id uuid NOT NULL,
    user_id uuid,
    element_id uuid,
    sharing boolean,
    access boolean,
    manager boolean,
    root boolean,
    deletion_date timestamp without time zone,
    expiration_date timestamp without time zone,
    status character varying(255) DEFAULT 'ACTIVE'::character varying NOT NULL
);


ALTER TABLE public.trash_record OWNER TO admin;

--
-- Name: user_deletion_process; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.user_deletion_process (
    user_id uuid NOT NULL,
    file_management_status character varying(255),
    file_sharing_status character varying(255),
    file_access_control_status character varying(255),
    user_management_status character varying(255),
    user_authentication_status character varying(255),
    trash_status character varying(255),
    sync_service_status character varying(255),
    created_at timestamp without time zone
);


ALTER TABLE public.user_deletion_process OWNER TO admin;

--
-- Name: user_entity; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.user_entity (
    id uuid NOT NULL,
    username character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL
);


ALTER TABLE public.user_entity OWNER TO admin;

--
-- Name: user_info; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.user_info (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    last_modified_date timestamp without time zone
);


ALTER TABLE public.user_info OWNER TO admin;

--
-- Name: access_rule access_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.access_rule
    ADD CONSTRAINT access_rule_pkey PRIMARY KEY (id);


--
-- Name: element_entity element_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.element_entity
    ADD CONSTRAINT element_entity_pkey PRIMARY KEY (id);


--
-- Name: file_entity file_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_entity
    ADD CONSTRAINT file_entity_pkey PRIMARY KEY (id);


--
-- Name: folder_entity folder_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.folder_entity
    ADD CONSTRAINT folder_entity_pkey PRIMARY KEY (id);


--
-- Name: shared_access shared_access_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.shared_access
    ADD CONSTRAINT shared_access_pkey PRIMARY KEY (id);


--
-- Name: snapshot_element_entity snapshot_element_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.snapshot_element_entity
    ADD CONSTRAINT snapshot_element_entity_pkey PRIMARY KEY (id);


--
-- Name: snapshot_entity snapshot_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.snapshot_entity
    ADD CONSTRAINT snapshot_entity_pkey PRIMARY KEY (id);


--
-- Name: trash_record trash_record_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.trash_record
    ADD CONSTRAINT trash_record_pkey PRIMARY KEY (id);


--
-- Name: user_deletion_process user_deletion_process_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_deletion_process
    ADD CONSTRAINT user_deletion_process_pkey PRIMARY KEY (user_id);


--
-- Name: user_entity user_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT user_entity_pkey PRIMARY KEY (id);


--
-- Name: user_entity user_entity_username_key; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT user_entity_username_key UNIQUE (username);


--
-- Name: user_info user_info_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.user_info
    ADD CONSTRAINT user_info_pkey PRIMARY KEY (id);


--
-- Name: file_entity file_entity_element_id_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_entity
    ADD CONSTRAINT file_entity_element_id_id_fkey FOREIGN KEY (element_id_id) REFERENCES public.element_entity(id);


--
-- Name: file_entity file_entity_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.file_entity
    ADD CONSTRAINT file_entity_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.folder_entity(id);


--
-- Name: folder_entity folder_entity_element_id_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.folder_entity
    ADD CONSTRAINT folder_entity_element_id_id_fkey FOREIGN KEY (element_id_id) REFERENCES public.element_entity(id);


--
-- Name: folder_entity folder_entity_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.folder_entity
    ADD CONSTRAINT folder_entity_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.folder_entity(id);


--
-- Name: snapshot_element_entity snapshot_element_entity_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.snapshot_element_entity
    ADD CONSTRAINT snapshot_element_entity_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.snapshot_element_entity(id) ON DELETE CASCADE;


--
-- Name: snapshot_element_entity snapshot_element_entity_snapshot_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.snapshot_element_entity
    ADD CONSTRAINT snapshot_element_entity_snapshot_id_fkey FOREIGN KEY (snapshot_id) REFERENCES public.snapshot_entity(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

