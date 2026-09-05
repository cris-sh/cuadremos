package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest (

    @NotBlank
    @Size(min = 3, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username invalid")
    String username,

    @NotBlank
    @Size(min = 2, max = 100)
    String name,

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(min = 8, max = 128)
    String password

) {}