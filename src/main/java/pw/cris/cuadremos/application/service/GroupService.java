package pw.cris.cuadremos.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.dto.MemberResponse;
import pw.cris.cuadremos.application.dto.UpdateGroupRequest;
import pw.cris.cuadremos.domain.exception.GroupAccessDeniedException;
import pw.cris.cuadremos.domain.exception.GroupArchivedException;
import pw.cris.cuadremos.domain.model.Group;
import pw.cris.cuadremos.domain.model.GroupMember;
import pw.cris.cuadremos.domain.model.GroupRole;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.GroupRepository;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + creatorId));

        Group group = Group.create(request.name(), creator);

        Group saved = groupRepository.save(group);
        return toResponse(saved);
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, UUID callerId, String username) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        // Check permissions first, so outsiders learn nothing about which usernames exist
        requireAdmin(group, callerId);
        requireActive(group);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (group.hasMember(user)) {
            throw new IllegalArgumentException("User is already a member of the group");
        }

        group.addMember(user, GroupRole.MEMBER);
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public void removeMember(UUID groupId, UUID callerId, UUID targetId) {
        Group group = findGroup(groupId);

        // Check membership first, so outsiders learn nothing about who belongs to the group
        GroupMember caller = requireMember(group, callerId);
        requireActive(group);
        GroupMember target = requireTarget(group, targetId);

        if (!callerId.equals(targetId)) {
            requireCanRemove(caller, target);
        } else if (target.isOwner()) {
            throw new IllegalArgumentException("Transfer ownership before leaving the group");
        }

        group.removeMember(target.getUser());
        groupRepository.save(group);
    }

    @Transactional
    public void updateMember(UUID groupId, UUID callerId, UUID targetId, GroupRole newRole) {
        Group group = findGroup(groupId);

        // Check membership first, so outsiders learn nothing about who belongs to the group
        GroupMember caller = requireMember(group, callerId);

        if (!caller.isOwner()) {
            throw new GroupAccessDeniedException("Only the owner can change member roles");
        }

        requireActive(group);
        GroupMember target = requireTarget(group, targetId);

        if (target.isOwner()) {
            throw new IllegalArgumentException("Cannot change the role of the owner");
        }

        target.setRole(newRole);
        groupRepository.save(group);
    }

    @Transactional
    public void transferOwnership(UUID groupId, UUID callerId, UUID targetId) {
        Group group = findGroup(groupId);

        // Check the caller first, so outsiders learn nothing about who belongs to the group
        GroupMember caller = requireMember(group, callerId);
        if (!caller.isOwner()) {
            throw new GroupAccessDeniedException("Only the owner can transfer ownership");
        }

        requireActive(group);
        GroupMember target = requireTarget(group, targetId);
        group.transferOwnershipTo(target);
        groupRepository.save(group);
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UUID callerId, UpdateGroupRequest request) {
        Group group = findGroup(groupId);
        requireAdmin(group, callerId);
        requireActive(group);

        // Only the fields that were sent change; the rest keep their current value
        if (request.name() != null) {
            group.rename(request.name());
        }
        if (request.icon() != null) {
            group.changeIcon(request.icon());
        }

        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public void archiveGroup(UUID groupId, UUID callerId) {
        Group group = findGroup(groupId);

        // Check the caller first, so outsiders learn nothing about the group
        GroupMember caller = requireMember(group, callerId);
        if (!caller.isOwner()) {
            throw new GroupAccessDeniedException("Only the owner can archive the group");
        }

        requireActive(group);
        group.archive();
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId, UUID callerId) {
        Group group = findGroup(groupId);
        requireMember(group, callerId);
        return toResponse(group);
    }

    private Group findGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
    }

    /* Every change goes through here; it runs after the permissions checks on purpose */
    private void requireActive(Group group) {
        if (group.isArchived()) {
            throw new GroupArchivedException("Group is archived and can no longer be changed");
        }
    }

    private GroupMember requireMember(Group group, UUID userId) {
        return group.findMember(userId)
                .orElseThrow(() -> new GroupAccessDeniedException("User is not a member of the group"));
    }

    /* Unlike requireMember, a missing target is a bad request, not a permission problem */
    private GroupMember requireTarget(Group group, UUID userId) {
        return group.findMember(userId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member of the group"));
    }

    private void requireAdmin(Group group, UUID userId) {
        if (!requireMember(group, userId).isAdmin()) {
            throw new GroupAccessDeniedException("User is not an admin of the group");
        }
    }

    /* Admins remove plain members, only the owner removes admins, and nobody removes the owner */
    private void requireCanRemove(GroupMember caller, GroupMember target) {
        if (!caller.isAdmin()) {
            throw new GroupAccessDeniedException("Only admins can remove members");
        }
        if (target.isOwner()) {
            throw new GroupAccessDeniedException("The owner of the group cannot be removed");
        }
        if (target.isAdmin() && !caller.isOwner()) {
            throw new GroupAccessDeniedException("Only the owner can remove admins");
        }
    }


    private GroupResponse toResponse(Group group) {
        Set<MemberResponse> members = group.getMembers().stream()
                .map(member -> new MemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getName(),
                        member.getRole(),
                        group.isOwnedBy(member.getUser())
                ))
                .collect(Collectors.toSet());

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getIcon(),
                members,
                group.getCreatedAt(),
                group.getArchivedAt()
        );
    }
}
