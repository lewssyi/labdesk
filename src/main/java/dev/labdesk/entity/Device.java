package dev.labdesk.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** 数据库实体；使用 snake_case JSON 保持现有前端与接口兼容。 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Device {
    private Long id;
    private String name;
    private boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

