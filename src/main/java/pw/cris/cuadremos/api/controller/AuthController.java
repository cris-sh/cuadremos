package pw.cris.cuadremos.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pw.cris.cuadremos.application.dto.LoginRequest;
import pw.cris.cuadremos.application.dto.RegisterRequest;
import pw.cris.cuadremos.application.dto.TokenResponse;
import pw.cris.cuadremos.application.dto.UserResponse;
import pw.cris.cuadremos.application.service.AuthService;
import pw.cris.cuadremos.application.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
