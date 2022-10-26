CREATE TABLE public.room_access_sync_tracker (
    room_id character varying(255) NOT NULL,
    rev character varying(255)
);

ALTER TABLE public.room_access_sync_tracker
    ADD CONSTRAINT room_access_sync_tracker_pkey PRIMARY KEY (room_id);
