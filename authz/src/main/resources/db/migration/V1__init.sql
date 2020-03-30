CREATE TABLE public.room_access (
    room_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    rev character varying(255),
    role character varying(255)
);

ALTER TABLE public.room_access
    ADD CONSTRAINT room_access_pkey PRIMARY KEY (room_id, user_id);
