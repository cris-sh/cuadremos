package pw.cris.cuadremos.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        Set<UserResponse> members,
        Instant createdAt
) {}
