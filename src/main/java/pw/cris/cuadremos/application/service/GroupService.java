package pw.cris.cuadremos.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.dto.MemberResponse;
import pw.cris.cuadremos.domain.exception.GroupAccessDeniedException;
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

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (group.hasMember(user)) {
            throw new IllegalArgumentException("User is already a member of the group");
        }

        group.addMember(user, GroupRole.MEMBER);
        return toResponse(groupRepository.save(group));
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

    private GroupMember requireMember(Group group, UUID userId) {
        return group.findMember(userId)
                .orElseThrow(() -> new GroupAccessDeniedException("User is not a member of the group"));
    }

    private void requireAdmin(Group group, UUID userId) {
        if (!requireMember(group, userId).isAdmin()) {
            throw new GroupAccessDeniedException("User is not an admin of the group");
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
                members,
                group.getCreatedAt()
        );
    }
}
