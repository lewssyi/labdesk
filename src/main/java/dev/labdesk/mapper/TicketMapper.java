package dev.labdesk.mapper;

import dev.labdesk.entity.Ticket;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketMapper {
    List<Ticket> findAllRecent();
    List<Ticket> findRecentByUser(@Param("userId") long userId);
    Ticket findById(@Param("id") long id);
    int insert(@Param("ticket") Ticket ticket);
    int updateStatusIfVersionMatches(@Param("id") long id, @Param("version") int version,
                                     @Param("from") String from, @Param("to") String to);
}
