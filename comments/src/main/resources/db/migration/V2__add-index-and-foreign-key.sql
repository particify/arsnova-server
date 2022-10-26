DELETE FROM vote WHERE comment_id NOT IN (SELECT id FROM comment);

ALTER TABLE vote
    ADD CONSTRAINT fk_vote_comment
    FOREIGN KEY (comment_id)
    REFERENCES comment (id)
    ON DELETE CASCADE;

CREATE INDEX idx_comment_room_id ON comment (room_id);
CREATE INDEX idx_comment_creator_id ON comment (creator_id);
