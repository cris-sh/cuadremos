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
import pw.cris.cuadremos.domain.exception.GroupAccessDeniedException;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private GroupRole roleOf(Group group, User user) {
        return group.findMember(user.getId()).orElseThrow().getRole();
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

        GroupResponse response = groupService.addMember(group.getId(), cris.getId(), "yuka");

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

        GroupResponse response = groupService.addMember(group.getId(), cris.getId(), "yuka");

        MemberResponse addedYuka = response.members().stream()
                .filter(member -> member.username().equals("yuka"))
                .findFirst()
                .orElseThrow();

        assertThat(addedYuka.role()).isEqualTo(GroupRole.MEMBER);
        assertThat(addedYuka.owner()).isFalse();
    }

    @Test
    @DisplayName("refuses to let a plain member add people")
    void refusesPlainMemberAddingPeople() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.addMember(group.getId(), yuka.getId(), "ana"))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("admin");

        verify(userRepository, never()).findByUsername(anyString());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let someone outside the group add people")
    void refusesOutsiderAddingPeople() {
        Group group = group(user("cris"));
        User outsider = user("outsider");
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.addMember(group.getId(), outsider.getId(), "ana"))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("not a member");

        verify(userRepository, never()).findByUsername(anyString());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails to add a member when the group does not exist")
    void failsWhenGroupDoesNotExist() {
        UUID ghostGroupId = UUID.randomUUID();
        when(groupRepository.findById(ghostGroupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(ghostGroupId, UUID.randomUUID(), "yuka"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails to add a member when the user does not exist")
    void failsWhenUserDoesNotExist() {
        User cris = user("cris");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(group.getId(), cris.getId(), "ghost"))
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

        assertThatThrownBy(() -> groupService.addMember(group.getId(), cris.getId(), "cris"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");

        verify(groupRepository, never()).save(any());
    }

    // --- removeMember ---

    @Test
    @DisplayName("lets an admin remove a plain member")
    void letsAdminRemoveMember() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.removeMember(group.getId(), cris.getId(), yuka.getId());

        assertThat(group.hasMember(yuka)).isFalse();
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("lets the owner remove an admin")
    void letsOwnerRemoveAdmin() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.removeMember(group.getId(), cris.getId(), ana.getId());

        assertThat(group.hasMember(ana)).isFalse();
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("lets a plain member leave the group")
    void letsMemberLeave() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.removeMember(group.getId(), yuka.getId(), yuka.getId());

        assertThat(group.hasMember(yuka)).isFalse();
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("lets an admin who is not the owner leave the group")
    void letsAdminLeave() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.removeMember(group.getId(), ana.getId(), ana.getId());

        assertThat(group.hasMember(ana)).isFalse();
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("refuses to let a plain member remove people")
    void refusesPlainMemberRemovingPeople() {
        User cris = user("cris");
        User yuka = user("yuka");
        User pepe = user("pepe");
        Group group = group(cris, yuka, pepe);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), yuka.getId(), pepe.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("Only admins");

        assertThat(group.hasMember(pepe)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let someone outside the group remove people")
    void refusesOutsiderRemovingPeople() {
        User cris = user("cris");
        User outsider = user("outsider");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), outsider.getId(), cris.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("not a member");

        assertThat(group.hasMember(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let an admin remove another admin")
    void refusesAdminRemovingAdmin() {
        User cris = user("cris");
        User ana = user("ana");
        User luis = user("luis");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        group.addMember(luis, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), ana.getId(), luis.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("Only the owner can remove admins");

        assertThat(group.hasMember(luis)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let anyone remove the owner")
    void refusesRemovingOwner() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), ana.getId(), cris.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("owner");

        assertThat(group.hasMember(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let the owner leave without transferring ownership first")
    void refusesOwnerLeaving() {
        User cris = user("cris");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), cris.getId(), cris.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transfer ownership");

        assertThat(group.hasMember(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails to remove someone who is not in the group")
    void failsWhenTargetIsNotAMember() {
        User cris = user("cris");
        User outsider = user("outsider");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.removeMember(group.getId(), cris.getId(), outsider.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target");

        verify(groupRepository, never()).save(any());
    }

    // --- updateMember ---
    @Test
    @DisplayName("lets the owner promote a plain member to admin")
    void letsOwnerPromoteMember() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.updateMember(group.getId(), cris.getId(), yuka.getId(), GroupRole.ADMIN);

        assertThat(roleOf(group, yuka)).isEqualTo(GroupRole.ADMIN);
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("lets the owner demote an admin to plain member")
    void letsOwnerDemoteAdmin() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.updateMember(group.getId(), cris.getId(), ana.getId(), GroupRole.MEMBER);

        assertThat(roleOf(group, ana)).isEqualTo(GroupRole.MEMBER);
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("accepts setting a role the member already has")
    void acceptsSameRole() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.updateMember(group.getId(), cris.getId(), ana.getId(), GroupRole.ADMIN);

        assertThat(roleOf(group, ana)).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    @DisplayName("refuses to let an admin who is not the owner change roles")
    void refusesAdminChangingRoles() {
        User cris = user("cris");
        User ana = user("ana");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateMember(group.getId(), ana.getId(), yuka.getId(), GroupRole.ADMIN))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("Only the owner");

        assertThat(roleOf(group, yuka)).isEqualTo(GroupRole.MEMBER);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let a plain member change roles")
    void refusesMemberChangingRoles() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateMember(group.getId(), yuka.getId(), yuka.getId(), GroupRole.ADMIN))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("Only the owner");

        assertThat(roleOf(group, yuka)).isEqualTo(GroupRole.MEMBER);
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let someone outside the group change roles")
    void refusesOutsiderChangingRoles() {
        User cris = user("cris");
        User yuka = user("yuka");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.updateMember(group.getId(), cris.getId(), ana.getId(), GroupRole.ADMIN);

        assertThat(roleOf(group, ana)).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    @DisplayName("fails to change the role of someone who is not in the group")
    void failsWhenRoleTargetIsNotAMember() {
        User cris = user("cris");
        User outsider = user("outsider");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateMember(group.getId(), cris.getId(), outsider.getId(), GroupRole.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to change the owner's own role")
    void refusesChangingOwnerRole() {
        User cris = user("cris");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.updateMember(group.getId(), cris.getId(), cris.getId(), GroupRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");

        assertThat(roleOf(group, cris)).isEqualTo(GroupRole.ADMIN);
        verify(groupRepository, never()).save(any());
    }

    // --- transferOwnership ---

    @Test
    @DisplayName("lets the owner hand the group over to an admin, staying on as admin")
    void letsOwnerTransferToAdmin() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.transferOwnership(group.getId(), cris.getId(), ana.getId());

        assertThat(group.isOwnedBy(ana)).isTrue();
        assertThat(roleOf(group, cris)).isEqualTo(GroupRole.ADMIN);
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("lets the former owner leave once ownership is transferred")
    void letsFormerOwnerLeave() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.transferOwnership(group.getId(), cris.getId(), ana.getId());
        groupService.removeMember(group.getId(), cris.getId(), cris.getId());

        assertThat(group.hasMember(cris)).isFalse();
    }

    @Test
    @DisplayName("refuses to hand the group over to a plain member")
    void refusesTransferToPlainMember() {
        User cris = user("cris");
        User yuka = user("yuka");
        Group group = group(cris, yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.transferOwnership(group.getId(), cris.getId(), yuka.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin");

        assertThat(group.isOwnedBy(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let an admin who is not the owner transfer ownership")
    void refusesAdminTransferring() {
        User cris = user("cris");
        User ana = user("ana");
        Group group = group(cris);
        group.addMember(ana, GroupRole.ADMIN);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.transferOwnership(group.getId(), ana.getId(), ana.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("Only the owner");

        assertThat(group.isOwnedBy(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to let someone outside the group transfer ownership")
    void refusesOutsiderTransferring() {
        User cris = user("cris");
        User outsider = user("outsider");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.transferOwnership(group.getId(), outsider.getId(), cris.getId()))
                .isInstanceOf(GroupAccessDeniedException.class)
                .hasMessageContaining("not a member");

        assertThat(group.isOwnedBy(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails to hand the group over to someone who is not in it")
    void failsWhenNewOwnerIsNotAMember() {
        User cris = user("cris");
        User outsider = user("outsider");
        Group group = group(cris);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.transferOwnership(group.getId(), cris.getId(), outsider.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target");

        assertThat(group.isOwnedBy(cris)).isTrue();
        verify(groupRepository, never()).save(any());
    }

    // --- getGroup ---

    @Test
    @DisplayName("lets any member see the group, not only admins")
    void letsPlainMemberSeeGroup() {
        User yuka = user("yuka");
        Group group = group(user("cris"), yuka);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        GroupResponse response = groupService.getGroup(group.getId(), yuka.getId());

        assertThat(response.id()).isEqualTo(group.getId());
        assertThat(response.name()).isEqualTo("trip to Cucuta");
        assertThat(response.members()).hasSize(2);
    }

    @Test
    @DisplayName("refuses to show the group to someone outside it")
    void refusesOutsiderSeeingGroup() {
        Group group = group(user("cris"));
        User outsider = user("outsider");
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.getGroup(group.getId(), outsider.getId()))
                .isInstanceOf(GroupAccessDeniedException.class);
    }

    @Test
    @DisplayName("fails to return a group that does not exist")
    void failsToReturnMissingGroup() {
        UUID groupGhostId = UUID.randomUUID();
        when(groupRepository.findById(groupGhostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(groupGhostId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}