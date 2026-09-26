-- A membership now carries data of its own (the role), so it gets its own id
ALTER TABLE group_members DROP CONSTRAINT group_members_pkey;
ALTER TABLE group_members ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE group_members ALTER COLUMN id DROP DEFAULT;
ALTER TABLE group_members ADD PRIMARY KEY (id);

-- Still one membership per user per group
ALTER TABLE group_members ADD CONSTRAINT group_members_group_user_key UNIQUE (group_id, user_id);

-- Existing groups never stored their creator, so everyone already inside becomes ADMIN
-- rather than leaving a group with nobody allowed to manage it
ALTER TABLE group_members ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE group_members ALTER COLUMN role DROP DEFAULT;
ALTER TABLE group_members ADD CONSTRAINT group_members_role_check CHECK (role IN ('ADMIN', 'MEMBER'));

-- The owner is a property of the group itself, so it lives on groups: exactly one per group
ALTER TABLE groups ADD COLUMN owner_id UUID REFERENCES users(id);

-- Existing groups never stored their creator: the member with the oldest account takes ownership
UPDATE groups g
SET owner_id = (
    SELECT gm.user_id
    FROM group_members gm
    JOIN users u ON u.id = gm.user_id
    WHERE gm.group_id = g.id
    ORDER BY u.created_at, u.id
    LIMIT 1
);

ALTER TABLE groups ALTER COLUMN owner_id SET NOT NULL;