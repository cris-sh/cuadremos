package pw.cris.cuadremos.application.dto;

import java.util.UUID;

public record UserResponse (
        UUID id,
        String username,
        String name,
        String email
) {}
