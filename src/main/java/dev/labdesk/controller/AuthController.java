package dev.labdesk.controller;

import dev.labdesk.dto.LoginRequest;
import dev.labdesk.security.BearerToken;
import dev.labdesk.service.AuthService;
import dev.labdesk.vo.LoginResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(BearerToken.extract(authorization));
        return Map.of("ok", true);
    }
}
