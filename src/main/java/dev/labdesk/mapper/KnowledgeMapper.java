package dev.labdesk.mapper;

import dev.labdesk.entity.Knowledge;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeMapper {
    List<Knowledge> findAll();
    int insertIfAbsent(@Param("id") long id, @Param("title") String title,
                       @Param("keywords") String keywords, @Param("content") String content);
}
