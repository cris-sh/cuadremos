package pw.cris.cuadremos.application.dto;

import pw.cris.cuadremos.domain.model.GroupRole;

import java.util.UUID;

public record MemberResponse(
        UUID id,
        String username,
        String name,
        GroupRole role,
        boolean owner
) {
}
