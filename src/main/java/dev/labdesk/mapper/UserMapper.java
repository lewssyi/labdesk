package dev.labdesk.mapper;

import dev.labdesk.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserAccount findById(@Param("id") long id);
    UserAccount findByUsername(@Param("username") String username);
    int insertIfAbsent(@Param("user") UserAccount user);
}
