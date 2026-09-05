package pw.cris.cuadremos.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pw.cris.cuadremos.application.dto.RegisterRequest;
import pw.cris.cuadremos.application.dto.UserResponse;
import pw.cris.cuadremos.domain.exception.EmailAlreadyExistsException;
import pw.cris.cuadremos.domain.exception.UsernameAlreadyExistsException;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .username(request.username().toLowerCase())
                .name(request.name())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);

        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getName(),
                saved.getEmail()
        );
    }

}
