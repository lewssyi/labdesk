package dev.labdesk.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 工具专用响应，不将其他预约人的身份或幂等标识交给模型。 */
public final class ReservationQueries {
    private ReservationQueries() {}
    public record Slot(LocalDateTime start, LocalDateTime end) {}
    public record Availability(long deviceId, String deviceName, boolean enabled,
                               LocalDateTime start, LocalDateTime end, boolean available,
                               List<Slot> occupied, List<Slot> free, String notice) {}
    public record MyBooking(long id, long deviceId, String deviceName, LocalDateTime start,
                            LocalDateTime end, String status) {}
    public record MyBookings(List<MyBooking> bookings, boolean truncated, String notice) {}
}
