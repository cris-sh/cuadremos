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
import pw.cris.cuadremos.application.service.GroupService;
import pw.cris.cuadremos.domain.exception.GroupAccessDeniedException;
import pw.cris.cuadremos.infrastructure.security.SecurityConfig;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(SecurityConfig.class)
class GroupControllerTest {

    private static final String CREATE_BODY = "{\"name\": \"Trip to Cucuta\"}";
    private static final String ADD_MEMBER_BODY = "{\"username\": \"yuka\"}";

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
                Set.of(),
                Instant.now()
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
                        UUID.randomUUID(), "Trip to Cucuta", Set.of(), Instant.now()
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

}
