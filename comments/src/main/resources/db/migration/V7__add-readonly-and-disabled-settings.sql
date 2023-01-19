ALTER TABLE settings
    ADD COLUMN readonly boolean DEFAULT FALSE,
    ADD COLUMN disabled boolean DEFAULT FALSE;
