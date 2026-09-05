package pw.cris.cuadremos.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @RequestHeader("X-User-Id") UUID userId
            ) {
        return groupService.createGroup(request, userId);
    }

    @PostMapping("/{groupId}/members")
    public GroupResponse addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        return groupService.addMember(groupId, request.username());
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }

}
