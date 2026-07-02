-- Remove UUID token column; authentication now uses the player's integer id
ALTER TABLE players DROP COLUMN IF EXISTS token;
