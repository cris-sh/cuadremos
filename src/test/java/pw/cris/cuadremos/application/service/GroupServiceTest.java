package pw.cris.cuadremos.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.dto.MemberResponse;
import pw.cris.cuadremos.domain.model.Group;
import pw.cris.cuadremos.domain.model.GroupRole;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.GroupRepository;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private User user(String username) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .name("Test User")
                .email(username + "@example.com")
                .password("hash")
                .build();
    }

    /* A group owned by its first user; the rest join as plain members */
    private Group group(User owner, User... members) {
        Group group = Group.create("trip to Cucuta", owner);
        group.setId(UUID.randomUUID());
        for (User member : members) {
            group.addMember(member, GroupRole.MEMBER);
        }
        return group;
    }

    /* Makes save() hand back whatever it receives, like a real repository would */
    private void saveReturnsItsArgument() {
        when(groupRepository.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));
    }

    // --- createGroup ---

    @Test
    @DisplayName("creates the group with the creator as its only member")
    void createsGroupWithCreatorAsMember() {
        User creator = user("cris");
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        saveReturnsItsArgument();

        GroupResponse response = groupService.createGroup(
                new CreateGroupRequest("trip to Cucuta"), creator.getId()
        );

        assertThat(response.name()).isEqualTo("trip to Cucuta");
        assertThat(response.members())
                .extracting(MemberResponse::username)
                .containsExactly("cris");
    }

    @Test
    @DisplayName("makes the creator the owner and an admin of the new group")
    void makesCreatorOwnerAndAdmin() {
        User creator = user("cris");
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        saveReturnsItsArgument();

        GroupResponse response = groupService.createGroup(
                new CreateGroupRequest("trip to Cucuta"), creator.getId()
        );

        MemberResponse cris = response.members().iterator().next();
        assertThat(cris.role()).isEqualTo(GroupRole.ADMIN);
        assertThat(cris.owner()).isTrue();
    }

    @Test
    @DisplayName("fails to create a group when the creator does not exist")
    void failsWhenCreatorDoesNotExist() {
        UUID ghostId = UUID.randomUUID();
        when(userRepository.findById(ghostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(new CreateGroupRequest("trip to Cucuta"), ghostId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    // --- addMember ---

    @Test
    @DisplayName("adds an existing user to the group")
    void addsExistingUser() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("yuka")).thenReturn(Optional.of(yuka));

        saveReturnsItsArgument();

        GroupResponse response = groupService.addMember(group.getId(), "yuka");

        assertThat(response.members())
                .extracting(MemberResponse::username)
                .containsExactlyInAnyOrder("cris", "yuka");
    }

    @Test
    @DisplayName("adds new users as plain members, not admins or owners")
    void addsNewUsersAsMembers() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("yuka")).thenReturn(Optional.of(yuka));
        saveReturnsItsArgument();

        GroupResponse response = groupService.addMember(group.getId(), "yuka");

        MemberResponse addedYuka = response.members().stream()
                .filter(member -> member.username().equals("yuka"))
                .findFirst()
                .orElseThrow();

        assertThat(addedYuka.role()).isEqualTo(GroupRole.MEMBER);
        assertThat(addedYuka.owner()).isFalse();
    }

    @Test
    @DisplayName("fails to add a member when the group does not exist")
    void failsWhenGroupDoesNotExist() {
        UUID ghostGroupId = UUID.randomUUID();
        when(groupRepository.findById(ghostGroupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(ghostGroupId, "yuka"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails to add a member when the user does not exist")
    void failsWhenUserDoesNotExist() {
        Group group = group(user("cris"));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(group.getId(), "ghost"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to add someone who is already a member")
    void refusesDuplicateMember() {
        User cris = user("cris");
        Group group = group(cris);

        // A separate object with the same id: what a fresh database lookup hands back
        User sameCrisFromDatabase = User.builder()
                .id(cris.getId())
                .username("cris")
                .build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("cris")).thenReturn(Optional.of(sameCrisFromDatabase));

        assertThatThrownBy(() -> groupService.addMember(group.getId(), "cris"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");

        verify(groupRepository, never()).save(any());
    }

    // --- getGroup ---

    @Test
    @DisplayName("returns the group with its members")
    void returnsGroupWithMembers() {
        Group group = group(user("cris"), user("yuka"));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        GroupResponse response = groupService.getGroup(group.getId());

        assertThat(response.id()).isEqualTo(group.getId());
        assertThat(response.name()).isEqualTo("trip to Cucuta");
        assertThat(response.members()).hasSize(2);
    }

    @Test
    @DisplayName("fails to return a group that does not exist")
    void failsToReturnMissingGroup() {
        UUID groupGhostId = UUID.randomUUID();
        when(groupRepository.findById(groupGhostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(groupGhostId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}