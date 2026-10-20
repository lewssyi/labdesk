package dev.labdesk.mapper;

import dev.labdesk.entity.Device;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceMapper {
    List<Device> findAll();
    Device findById(@Param("id") long id);
    Device findByIdForUpdate(@Param("id") long id);
    int insertIfAbsent(@Param("id") long id, @Param("name") String name);
}
