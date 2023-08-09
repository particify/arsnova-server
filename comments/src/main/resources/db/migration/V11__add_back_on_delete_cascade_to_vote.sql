ALTER TABLE vote
    DROP CONSTRAINT vote_comment_id_fkey;

ALTER TABLE vote
    ADD FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE;
