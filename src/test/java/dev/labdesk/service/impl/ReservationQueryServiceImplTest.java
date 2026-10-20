package dev.labdesk.service.impl;

import dev.labdesk.common.ApiError;
import dev.labdesk.entity.Device;
import dev.labdesk.mapper.*;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.vo.ReservationQueries.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationQueryServiceImplTest {
    DeviceMapper devices = mock(DeviceMapper.class);
    BookingMapper bookings = mock(BookingMapper.class);
    ReservationQueryServiceImpl service = new ReservationQueryServiceImpl(devices, bookings);
    LocalDateTime start = LocalDateTime.of(2090, 1, 1, 9, 0);
    LocalDateTime end = start.plusHours(8);

    Device device(boolean enabled) {
        var d = new Device(); d.setId(2L); d.setName("显微镜"); d.setEnabled(enabled);
        when(devices.findById(2)).thenReturn(d); return d;
    }

    @Test void computesClippedMergedOccupiedAndFreeIntervals() {
        device(true);
        when(bookings.findOccupiedSlots(2, start, end)).thenReturn(List.of(
                new Slot(start.minusHours(1), start.plusHours(1)),
                new Slot(start.plusHours(1), start.plusHours(2)),
                new Slot(start.plusHours(4), end.plusHours(1))));
        var result = service.availability(2, start.toString(), end.toString());
        assertFalse(result.available());
        assertEquals(List.of(new Slot(start, start.plusHours(2)), new Slot(start.plusHours(4), end)), result.occupied());
        assertEquals(List.of(new Slot(start.plusHours(2), start.plusHours(4))), result.free());
        verify(bookings, never()).findAllRecent();
    }

    @Test void distinguishesFreeAndDisabledDevice() {
        device(true);
        when(bookings.findOccupiedSlots(2, start, end)).thenReturn(List.of());
        assertTrue(service.availability(2, start.toString(), end.toString()).available());
        device(false);
        var result = service.availability(2, start.toString(), end.toString());
        assertFalse(result.available()); assertTrue(result.free().isEmpty());
    }

    @Test void validatesTimesBeforeDatabaseQuery() {
        assertThrows(ApiError.class, () -> service.availability(2, "明天下午", "四点"));
        assertThrows(ApiError.class, () -> service.availability(2, end.toString(), start.toString()));
        assertThrows(ApiError.class, () -> service.availability(2, start.toString(), start.plusDays(8).toString()));
        assertThrows(ApiError.class, () -> service.availability(2, "2000-01-01T09:00:00", "2000-01-01T10:00:00"));
        verifyNoInteractions(bookings, devices);
    }

    @Test void adminOwnBookingsAreScopedAndTruncationIsExplicit() {
        var row = new MyBooking(1, 2, "显微镜", start, end, "CONFIRMED");
        when(bookings.findOwnInRange(99, null, null)).thenReturn(Collections.nCopies(101, row));
        var result = service.myBookings(new CurrentUser(99, "admin", "ADMIN"), null, null);
        assertTrue(result.truncated()); assertEquals(100, result.bookings().size());
        verify(bookings).findOwnInRange(99, null, null);
        verify(bookings, never()).findAllRecent();
        assertThrows(ApiError.class, () -> service.myBookings(new CurrentUser(1, "s", "STUDENT"), start.toString(), null));
    }
}
