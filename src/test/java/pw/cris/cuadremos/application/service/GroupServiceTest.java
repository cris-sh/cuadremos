package pw.cris.cuadremos.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.domain.exception.NotGroupMemberException;
import pw.cris.cuadremos.domain.model.Group;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.GroupRepository;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, userRepository);
    }

    private User user(UUID id, String username) {
        return User.builder().id(id).username(username).build();
    }

    @Test
    @DisplayName("adds the caller as a member when creating a group")
    void addsCallerAsMemberOnCreate() {
        UUID creatorId = UUID.randomUUID();
        User creator = user(creatorId, "cris");
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(groupRepository.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

        var response = groupService.createGroup(new CreateGroupRequest("Trip to Cucuta"), creatorId);

        assertThat(response.members()).hasSize(1);
    }

    @Test
    @DisplayName("rejects addMember when the caller is not in the group")
    void rejectsAddMemberFromNonMember() {
        UUID groupId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).build();
        group.getMembers().add(user(UUID.randomUUID(), "member"));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.addMember(groupId, outsiderId, "newguy"))
                .isInstanceOf(NotGroupMemberException.class);
    }

    @Test
    @DisplayName("allows an existing member to add a new member")
    void allowsMemberToAddNewMember() {
        UUID groupId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).build();
        group.getMembers().add(user(callerId, "cris"));
        User newMember = user(UUID.randomUUID(), "newguy");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("newguy")).thenReturn(Optional.of(newMember));
        when(groupRepository.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

        var response = groupService.addMember(groupId, callerId, "newguy");

        assertThat(response.members()).hasSize(2);
    }

    @Test
    @DisplayName("rejects getGroup when the caller is not in the group")
    void rejectsGetGroupFromNonMember() {
        UUID groupId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).build();
        group.getMembers().add(user(UUID.randomUUID(), "member"));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.getGroup(groupId, outsiderId))
                .isInstanceOf(NotGroupMemberException.class);
    }
}
