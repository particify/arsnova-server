--
-- PostgreSQL database dump
--

-- Dumped from database version 13.1
-- Dumped by pg_dump version 13.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE public.bonus_token (
    comment_id character varying(255) NOT NULL,
    room_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    "timestamp" timestamp without time zone,
    token character varying(255)
);

ALTER TABLE public.bonus_token OWNER TO arsnovacomment;

CREATE TABLE public.comment (
    id character varying(255) NOT NULL,
    ack boolean NOT NULL,
    answer text,
    body text,
    correct integer NOT NULL,
    creator_id character varying(255),
    favorite boolean NOT NULL,
    read boolean NOT NULL,
    room_id character varying(255),
    tag character varying(255),
    "timestamp" timestamp without time zone
);

ALTER TABLE public.comment OWNER TO arsnovacomment;

CREATE TABLE public.settings (
    room_id character varying(255) NOT NULL,
    direct_send boolean
);

ALTER TABLE public.settings OWNER TO arsnovacomment;

CREATE TABLE public.vote (
    comment_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    vote integer NOT NULL
);

ALTER TABLE public.vote OWNER TO arsnovacomment;

ALTER TABLE ONLY public.bonus_token
    ADD CONSTRAINT bonus_token_pkey PRIMARY KEY (comment_id, room_id, user_id);

ALTER TABLE ONLY public.comment
    ADD CONSTRAINT comment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (room_id);

ALTER TABLE ONLY public.vote
    ADD CONSTRAINT vote_pkey PRIMARY KEY (comment_id, user_id);
