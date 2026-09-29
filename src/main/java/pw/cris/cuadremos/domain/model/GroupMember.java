package pw.cris.cuadremos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** One user's seat in one group, and the role that comes with it. */
@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    public boolean isAdmin() {
        return role == GroupRole.ADMIN;
    }

    public boolean isOwner() {
        return group.getOwner().equals(user);
    }
}
