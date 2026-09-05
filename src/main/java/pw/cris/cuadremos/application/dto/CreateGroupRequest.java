package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String name
) {
}
