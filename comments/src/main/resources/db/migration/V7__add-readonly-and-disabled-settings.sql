ALTER TABLE settings
    ADD COLUMN readonly boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN disabled boolean NOT NULL DEFAULT FALSE;
