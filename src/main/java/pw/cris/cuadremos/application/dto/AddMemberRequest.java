package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(

        @NotBlank
        String username
) {
}
