package pw.cris.cuadremos.domain.model;

import jakarta.persistence.*;
import lombok.*;
import pw.cris.cuadremos.domain.exception.InvitationNotPendingException;

import java.time.Instant;
import java.util.UUID;

/** An offer to join a group; the invitee only becomes a member once they accept it. */
@Entity
@Table(name = "group_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /* Set the moment the invitation leaves PENDING, whichever way it goes */
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /* Every invitation starts out pending; there is no other way to build one */
    public static GroupInvitation create(Group group, User invitee, User inviter) {
        GroupInvitation invitation = new GroupInvitation();
        invitation.group = group;
        invitation.invitee = invitee;
        invitation.inviter = inviter;
        invitation.status = InvitationStatus.PENDING;
        return invitation;
    }

    /* Accepting and joining happen together, so one can never happen without the other */
    public void accept() {
        resolve(InvitationStatus.ACCEPTED);
        group.addMember(invitee, GroupRole.MEMBER);
    }

    public void decline() {
        resolve(InvitationStatus.DECLINED);
    }

    public void cancel() {
        resolve(InvitationStatus.CANCELLED);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    private void resolve(InvitationStatus newStatus) {
        if (!isPending()) {
            throw new InvitationNotPendingException("Invitation is already " + status.name().toLowerCase());
        }
        this.status = newStatus;
        this.resolvedAt = Instant.now();
    }
}