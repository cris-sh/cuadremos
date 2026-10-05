package pw.cris.cuadremos.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial update: a field left out (or null) keeps its current value.
 * An empty icon ("") removes the icon
 */
public record UpdateGroupRequest(
        @Size(min = 2, max = 100)
        String name,

        @Size(max = 16)
        String icon
) {
}
