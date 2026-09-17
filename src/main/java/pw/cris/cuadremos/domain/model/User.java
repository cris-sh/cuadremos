package pw.cris.cuadremos.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 3, max = 32)
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "Can only contain letters, numbers, dots and underscores"
    )
    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
