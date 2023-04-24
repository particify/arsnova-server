UPDATE room_access SET role = 'OWNER' WHERE role = 'CREATOR';
UPDATE room_access SET role = 'MODERATOR' WHERE role = 'EXECUTIVE_MODERATOR';
