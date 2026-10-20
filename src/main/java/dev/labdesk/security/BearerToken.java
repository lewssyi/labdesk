package dev.labdesk.security;

public final class BearerToken {
    private BearerToken() {}

    public static String extract(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
    }
}
