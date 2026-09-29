package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull UUID memberId
) {
}
