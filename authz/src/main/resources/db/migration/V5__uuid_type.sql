ALTER TABLE room_access
    ALTER COLUMN room_id TYPE UUID USING room_id::uuid,
    ALTER COLUMN user_id TYPE UUID USING user_id::uuid;

ALTER TABLE room_access_sync_tracker
    ALTER COLUMN room_id TYPE UUID USING room_id::uuid;
