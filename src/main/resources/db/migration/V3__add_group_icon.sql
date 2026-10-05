-- Optional emoji shown next to the group name; existing groups simply have none
ALTER TABLE groups ADD COLUMN icon VARCHAR(16);