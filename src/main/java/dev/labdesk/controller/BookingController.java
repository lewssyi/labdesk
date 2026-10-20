package dev.labdesk.controller;

import dev.labdesk.dto.ReserveRequest;
import dev.labdesk.dto.VersionRequest;
import dev.labdesk.entity.Booking;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> list(@RequestAttribute("currentUser") CurrentUser user) {
        return bookingService.list(user);
    }

    @PostMapping
    public Booking reserve(@RequestAttribute("currentUser") CurrentUser user,
                           @Valid @RequestBody ReserveRequest request,
                           @RequestHeader(value = "Idempotency-Key", required = false) String requestKey) {
        return bookingService.reserve(user, request, requestKey);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Boolean> cancel(@RequestAttribute("currentUser") CurrentUser user,
                                      @PathVariable long id, @Valid @RequestBody VersionRequest request) {
        bookingService.cancel(user, id, request.version());
        return Map.of("ok", true);
    }
}
