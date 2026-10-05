package pw.cris.cuadremos.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String icon,
        Set<MemberResponse> members,
        Instant createdAt,
        Instant archivedAt
) {}
