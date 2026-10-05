-- Archiving freezes a group instead of deleting it, so its expenses and history survive
ALTER TABLE groups ADD COLUMN archived_at TIMESTAMPTZ;
