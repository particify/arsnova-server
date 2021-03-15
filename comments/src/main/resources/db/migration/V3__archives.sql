CREATE TABLE archive (
    id character varying(255) NOT NULL,
    room_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (room_id, name)
);

ALTER TABLE comment ADD COLUMN archive_id character varying(255);
ALTER TABLE comment
    ADD CONSTRAINT comment_archive_id_fkey
    FOREIGN KEY (archive_id)
    REFERENCES archive (id)
    ON DELETE CASCADE;
