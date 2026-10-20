package dev.labdesk.mapper;

import dev.labdesk.entity.Booking;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookingMapper {
    List<dev.labdesk.vo.ReservationQueries.Slot> findOccupiedSlots(@Param("deviceId") long deviceId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<dev.labdesk.vo.ReservationQueries.MyBooking> findOwnInRange(@Param("userId") long userId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Booking> findAllRecent();
    List<Booking> findRecentByUser(@Param("userId") long userId);
    Booking findById(@Param("id") long id);
    Booking findByRequestKey(@Param("userId") long userId, @Param("key") String key);
    int countOverlaps(@Param("deviceId") long deviceId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);
    int insert(@Param("booking") Booking booking);
    int cancelIfVersionMatches(@Param("id") long id, @Param("version") int version);
}
