package dev.labdesk.service.impl;

import dev.labdesk.common.ApiError;
import dev.labdesk.dto.LoginRequest;
import dev.labdesk.mapper.SessionMapper;
import dev.labdesk.mapper.UserMapper;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.AuthService;
import dev.labdesk.vo.LoginResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final SessionMapper sessionMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper, SessionMapper sessionMapper) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        var user = userMapper.findByUsername(request.username());
        ApiError.require(user != null && encoder.matches(request.password(), user.getPassword()),
                401, "账号或密码错误");
        String token = UUID.randomUUID().toString();
        sessionMapper.save(token, user.getId(), Duration.ofHours(2));
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }

    @Override
    public CurrentUser authenticate(String token) {
        Long userId = sessionMapper.findUserId(token);
        ApiError.require(userId != null, 401, "请先登录");
        var user = userMapper.findById(userId);
        ApiError.require(user != null, 401, "账号已失效");
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public void logout(String token) {
        sessionMapper.delete(token);
    }
}
