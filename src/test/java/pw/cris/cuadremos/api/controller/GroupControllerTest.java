package pw.cris.cuadremos.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.dto.UpdateGroupRequest;
import pw.cris.cuadremos.application.service.GroupService;
import pw.cris.cuadremos.domain.exception.GroupAccessDeniedException;
import pw.cris.cuadremos.domain.exception.GroupArchivedException;
import pw.cris.cuadremos.domain.model.GroupRole;
import pw.cris.cuadremos.infrastructure.security.SecurityConfig;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(SecurityConfig.class)
class GroupControllerTest {

    private static final String CREATE_BODY = "{\"name\": \"Trip to Cucuta\"}";
    private static final String ADD_MEMBER_BODY = "{\"username\": \"yuka\"}";
    private static final String PROMOTE_BODY = "{\"role\": \"ADMIN\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    // Required to build the filter chain, not exercised by these tests.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private GroupResponse someGroup() {
        return new GroupResponse(
                UUID.randomUUID(),
                "Trip to Cucuta",
                "",
                Set.of(),
                Instant.now(),
                null
        );
    }

    @Test
    @DisplayName("rejects group creation when no token is sent")
    void rejectsGroupCreationWithoutToken() throws Exception {
        mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(groupService, never()).createGroup(any(), any());
    }

    @Test
    @DisplayName("takes the creator id from the token subject")
    void takesCreatorIdFromTokenSubject() throws Exception {
        UUID userId = UUID.randomUUID();
        when(groupService.createGroup(any(), eq(userId)))
                .thenReturn(new GroupResponse(
                        UUID.randomUUID(), "Trip to Cucuta", "", Set.of(), Instant.now(), null
                ));

        mockMvc.perform(post("/api/groups")
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY)
        ).andExpect(status().isCreated());

        verify(groupService).createGroup(any(CreateGroupRequest.class), eq(userId));
    }

    @Test
    @DisplayName("tells the service who is asking to see a group")
    void passesCallerWhenGettingGroup() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(groupService.getGroup(groupId, userId)).thenReturn(someGroup());

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
        ).andExpect(status().isOk());

        verify(groupService).getGroup(groupId, userId);
    }

    @Test
    @DisplayName("tells the service who is asking to add a member")
    void passesCallerWhenAddingMember() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(groupService.addMember(groupId, userId, "yuka")).thenReturn(someGroup());

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADD_MEMBER_BODY)
        ).andExpect(status().isOk());

        verify(groupService).addMember(groupId, userId, "yuka");
    }

    @Test
    @DisplayName("answers 403 with the reason when the caller lacks permission")
    void answersForbiddenWhenAccessIsDenied() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(groupService.addMember(groupId, userId, "yuka"))
                .thenThrow(new GroupAccessDeniedException("User is not an admin of the group"));

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADD_MEMBER_BODY)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not an admin of the group"));
    }

    @Test
    @DisplayName("answers 204 and tells the service who is removing whom")
    void passesCallerAndTargetWhenRemovingMember() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/groups/{groupId}/members/{memberId}", groupId, targetId)
                .with(jwt().jwt(token -> token.subject(callerId.toString())))
        ).andExpect(status().isNoContent());

        verify(groupService).removeMember(groupId, callerId, targetId);
    }

    @Test
    @DisplayName("rejects removing a member when no token is sent")
    void rejectsRemovingMemberWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/groups/{groupId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verify(groupService, never()).removeMember(any(), any(), any());
    }

    @Test
    @DisplayName("answers 400 with the reason when the owner tries to leave")
    void answersBadRequestWhenOwnerLeaves() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Transfer ownership before leaving the group"))
                .when(groupService).removeMember(groupId, userId, userId);

        mockMvc.perform(delete("/api/groups/{groupId}/members/{memberId}", groupId, userId)
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transfer ownership before leaving the group"));
    }

    @Test
    @DisplayName("answers 204 and tells the service who changes which role")
    void passesCallerTargetAndRoleWhenUpdatingMember() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(patch("/api/groups/{groupId}/members/{memberId}", groupId, memberId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROMOTE_BODY)
        ).andExpect(status().isNoContent());

        verify(groupService).updateMember(groupId, userId, memberId, GroupRole.ADMIN);
    }

    @Test
    @DisplayName("rejects changing a role when no token is sent")
    void rejectsUpdatingMemberWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/groups/{groupId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROMOTE_BODY)
        ).andExpect(status().isUnauthorized());

        verify(groupService, never()).updateMember(any(), any(), any(), any());
    }

    @Test
    @DisplayName("answers 400 when the role is missing")
    void answersBadRequestWhenRoleIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/groups/{groupId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest());

        verify(groupService, never()).updateMember(any(), any(), any(), any());
    }

    @Test
    @DisplayName("answers 400 when the role does not exist")
    void answersBadRequestWhenRoleIsUnknown() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/groups/{groupId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"SUPERADMIN\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(groupService, never()).updateMember(any(), any(), any(), any());
    }

    @Test
    @DisplayName("answers 400 when a path id is not a valid UUID")
    void answersBadRequestWhenPathIdIsNotUuid() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/groups/{groupId}", "not-a-uuid")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'groupId': expected type UUID"));

        verify(groupService, never()).getGroup(any(), any());
    }

    @Test
    @DisplayName("answers 204 and tells the service who hands the group to whom")
    void passesCallerAndNewOwnerWhenTransferring() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(put("/api/groups/{groupId}/owner", groupId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": \"" + memberId + "\"}")
        ).andExpect(status().isNoContent());

        verify(groupService).transferOwnership(groupId, userId, memberId);
    }

    @Test
    @DisplayName("rejects transferring ownership when no token is sent")
    void rejectsTransferWithoutToken() throws Exception {
        mockMvc.perform(put("/api/groups/{groupId}/owner", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": \"" + UUID.randomUUID() + "\"}")
        ).andExpect(status().isUnauthorized());

        verify(groupService, never()).transferOwnership(any(), any(), any());
    }

    @Test
    @DisplayName("answers 400 when the new owner is missing")
    void answersBadRequestWhenNewOwnerIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/groups/{groupId}/owner", UUID.randomUUID())
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest());

        verify(groupService, never()).transferOwnership(any(), any(), any());
    }

    @Test
    @DisplayName("tells the service who edits which group and with what")
    void passesCallerAndChangesWhenUpdatingGroup() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(groupService.updateGroup(eq(groupId), eq(userId), any())).thenReturn(someGroup());

        mockMvc.perform(patch("/api/groups/{groupId}", groupId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Beach trip\", \"icon\": \"🏖️\"}")
        ).andExpect(status().isOk());

        verify(groupService).updateGroup(groupId, userId, new UpdateGroupRequest("Beach trip", "🏖️"));
    }

    @Test
    @DisplayName("answers 400 when the new name is too long")
    void answersBadRequestWhenNameIsTooLong() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/groups/{groupId}", UUID.randomUUID())
                .with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"" + "a".repeat(101) + "\"}")
        ).andExpect(status().isBadRequest());

        verify(groupService, never()).updateGroup(any(), any(), any());
    }

    @Test
    @DisplayName("answers 204 and tells the service who archives which group")
    void passesCallerWhenArchivingGroup() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                .with(jwt().jwt(token -> token.subject(userId.toString())))
        ).andExpect(status().isNoContent());

        verify(groupService).archiveGroup(groupId, userId);
    }

    @Test
    @DisplayName("rejects archiving a group when no token is sent")
    void rejectsArchivingWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/groups/{groupId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verify(groupService, never()).archiveGroup(any(), any());
    }

    @Test
    @DisplayName("answers 409 when the group is archived")
    void answersConflictWhenGroupIsArchived() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(groupService.updateGroup(eq(groupId), eq(userId), any()))
                .thenThrow(new GroupArchivedException("Group is archived and can no longer be changed"));

        mockMvc.perform(patch("/api/groups/{groupId}", groupId)
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Beach trip\"}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Group is archived and can no longer be changed"));
    }


}
