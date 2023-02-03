UPDATE settings SET file_upload_enabled = false WHERE file_upload_enabled IS NULL;

ALTER TABLE settings
    ALTER COLUMN direct_send SET DEFAULT true,
    ALTER COLUMN direct_send SET NOT NULL,
    ALTER COLUMN file_upload_enabled SET NOT NULL;
