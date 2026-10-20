package dev.labdesk.mapper;

import dev.labdesk.entity.TicketEvent;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketEventMapper {
    List<TicketEvent> findByTicketId(@Param("ticketId") long ticketId);
    int insert(@Param("ticketId") long ticketId, @Param("actorId") long actorId,
               @Param("from") String from, @Param("to") String to, @Param("note") String note);
}
