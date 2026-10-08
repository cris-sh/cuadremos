-- Invitations live apart from group_members, so a pending invitee is never counted as a member
CREATE TABLE group_invitations (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    invitee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT group_invitations_status_check
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    -- A pending invitation has no resolution time yet; every other one has
    CONSTRAINT group_invitations_resolved_check
        CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);

-- At most one pending invitation per user per group; past ones are kept as history
CREATE UNIQUE INDEX group_invitations_one_pending_idx
    ON group_invitations (group_id, invitee_id)
    WHERE status = 'PENDING';

-- Speeds up "show me my pending invitations"
CREATE INDEX group_invitations_invitee_pending_idx
    ON group_invitations (invitee_id)
    WHERE status = 'PENDING';