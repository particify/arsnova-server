ALTER TABLE room_access
    ADD COLUMN creation_timestamp TIMESTAMP DEFAULT NOW();
ALTER TABLE room_access
    ADD COLUMN last_access TIMESTAMP DEFAULT NOW();
