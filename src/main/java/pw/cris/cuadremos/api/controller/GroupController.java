package pw.cris.cuadremos.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pw.cris.cuadremos.application.dto.*;
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

    @PatchMapping("/{groupId}")
    public GroupResponse updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return groupService.updateGroup(groupId, currentUserId(jwt), request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveGroup(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        groupService.archiveGroup(groupId, currentUserId(jwt));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        groupService.removeMember(groupId, currentUserId(jwt), memberId);
    }

    @PatchMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        groupService.updateMember(groupId, currentUserId(jwt), memberId, request.role());
    }

    @PutMapping("/{groupId}/owner")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(
            @PathVariable UUID groupId,
            @Valid @RequestBody TransferOwnershipRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        groupService.transferOwnership(groupId, currentUserId(jwt), request.memberId());
    }

    /** The token subject carries the authenticated user's id */
    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

}
