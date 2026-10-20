package dev.labdesk.mapper;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** Redis 会话的数据访问层；MyBatis Mapper 仅负责 MySQL。 */
@Repository
public class SessionMapper {
    private static final String PREFIX = "labdesk:session:";
    private final StringRedisTemplate redis;

    public SessionMapper(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String token, long userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + token, Long.toString(userId), ttl);
    }

    public Long findUserId(String token) {
        if (token == null || token.isBlank()) return null;
        String value = redis.opsForValue().get(PREFIX + token);
        return value == null ? null : Long.valueOf(value);
    }

    public void delete(String token) {
        if (token != null && !token.isBlank()) redis.delete(PREFIX + token);
    }
}
