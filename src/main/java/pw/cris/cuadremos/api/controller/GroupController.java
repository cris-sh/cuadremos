package pw.cris.cuadremos.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pw.cris.cuadremos.application.dto.AddMemberRequest;
import pw.cris.cuadremos.application.dto.CreateGroupRequest;
import pw.cris.cuadremos.application.dto.GroupResponse;
import pw.cris.cuadremos.application.service.GroupService;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        return groupService.createGroup(request, currentUserId(jwt));
    }

    @PostMapping("/{groupId}/members")
    public GroupResponse addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return groupService.addMember(groupId, currentUserId(jwt), request.username());
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        return groupService.getGroup(groupId, currentUserId(jwt));
    }

    /** The token subject carries the authenticated user's id */
    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

}
