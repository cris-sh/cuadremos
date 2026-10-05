package pw.cris.cuadremos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /* Optional emoji shown next to the name */
    @Column(length = 16)
    private String icon;

    /* The owner is always an admin as well; ownership only adds owner-only powers on top */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMember> members = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /* Set when the owner archives the group: members can still read it, nobody can change it */
    private Instant archivedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * The one way to start a group: its creator owns it and joins it as an admin,
     * so a group can never exist with an owner who is not also one of its admins.
     */
    public static Group create(String name, User owner) {
        Group group = Group.builder()
                .name(name)
                .owner(owner)
                .build();
        group.addMember(owner, GroupRole.ADMIN);
        return group;
    }

    public void rename(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be blank");
        }
        this.name = name.strip();
    }

    /* A blank icon removes it, so there is always a way back to no icon */
    public void changeIcon(String icon) {
        this.icon = icon.isBlank() ? null : icon.strip();
    }

    public void archive() {
        this.archivedAt = Instant.now();
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isOwnedBy(User user) {
        return owner.equals(user);
    }

    /* Ownership only moves to an admin, so the owner is always an admin as well */
    public void transferOwnershipTo(GroupMember member) {
        if (!member.isAdmin()) {
            throw new IllegalArgumentException("Ownership can only be transferred to an admin");
        }
        this.owner = member.getUser();
    }

    public void addMember(User user, GroupRole role) {
        members.add(GroupMember.builder()
                .group(this)
                .user(user)
                .role(role)
                .build());
    }

    public void removeMember(User user) {
        members.removeIf(member -> member.getUser().equals(user));
    }

    public boolean hasMember(User user) {
        return members.stream().anyMatch(member -> member.getUser().equals(user));
    }

    /**
     * The membership of the user with this id, if they belong to the group.
     */
    public Optional<GroupMember> findMember(UUID userId) {
        return members.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst();
    }
}
