package dev.labdesk.service;

import dev.labdesk.dto.ReserveRequest;
import dev.labdesk.entity.Booking;
import dev.labdesk.security.CurrentUser;
import java.util.List;

public interface BookingService {
    List<Booking> list(CurrentUser actor);
    Booking reserve(CurrentUser actor, ReserveRequest request, String requestKey);
    void cancel(CurrentUser actor, long id, int version);
}
