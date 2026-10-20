package dev.labdesk.service;

import dev.labdesk.entity.Device;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.vo.ReservationQueries.*;
import java.util.List;

public interface ReservationQueryService {
    List<Device> searchDevices(String name);
    Availability availability(long deviceId, String start, String end);
    MyBookings myBookings(CurrentUser actor, String start, String end);
}
