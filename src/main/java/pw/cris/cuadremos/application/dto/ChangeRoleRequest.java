package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.NotNull;
import pw.cris.cuadremos.domain.model.GroupRole;

public record ChangeRoleRequest(
        @NotNull GroupRole role
        ) {
}
