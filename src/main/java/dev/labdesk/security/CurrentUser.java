package dev.labdesk.security;

/** 由认证服务生成的可信身份；不从请求 JSON 中接收用户 ID 或角色。 */
public record CurrentUser(long id, String username, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
