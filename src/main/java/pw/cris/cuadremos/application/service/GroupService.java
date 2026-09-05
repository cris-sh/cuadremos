package pw.cris.cuadremos.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.dto.UserResponse;
import pw.cris.cuadremos.domain.model.Group;
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

        Group group = Group.builder()
                .name(request.name())
                .build();

        group.getMembers().add(creator);

        Group saved = groupRepository.save(group);
        return toResponse(saved);
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, String username) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (group.getMembers().contains(user)) {
            throw new IllegalArgumentException("User is already a member of the group");
        }

        group.getMembers().add(user);
        return toResponse(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
        return toResponse(group);
    }


    private GroupResponse toResponse(Group group) {
        Set<UserResponse> members = group.getMembers().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getEmail()
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
