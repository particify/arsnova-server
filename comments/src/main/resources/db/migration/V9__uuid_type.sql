ALTER TABLE vote
    DROP CONSTRAINT fk_vote_comment,
    ALTER COLUMN comment_id TYPE UUID USING comment_id::uuid,
    ALTER COLUMN user_id TYPE UUID USING user_id::uuid;

ALTER TABLE comment
    DROP CONSTRAINT comment_archive_id_fkey,
    ALTER COLUMN id TYPE UUID USING id::uuid,
    ALTER COLUMN creator_id TYPE UUID USING creator_id::uuid,
    ALTER COLUMN room_id TYPE UUID USING room_id::uuid,
    ALTER COLUMN archive_id TYPE UUID USING archive_id::uuid;

ALTER TABLE archive
    ALTER COLUMN id TYPE UUID USING id::uuid,
    ALTER COLUMN room_id TYPE UUID USING room_id::uuid;

ALTER TABLE settings
    ALTER COLUMN room_id TYPE UUID USING room_id::uuid;

ALTER TABLE comment
    ADD FOREIGN KEY (archive_id) REFERENCES archive(id);

ALTER TABLE vote
    ADD FOREIGN KEY (comment_id) REFERENCES comment(id);
