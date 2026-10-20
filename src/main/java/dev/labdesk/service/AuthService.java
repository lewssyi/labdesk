package dev.labdesk.service;

import dev.labdesk.dto.LoginRequest;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    CurrentUser authenticate(String token);
    void logout(String token);
}
