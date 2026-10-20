package dev.labdesk.controller;

import dev.labdesk.dto.CreateTicketRequest;
import dev.labdesk.dto.TransitionRequest;
import dev.labdesk.entity.Ticket;
import dev.labdesk.entity.TicketEvent;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<Ticket> list(@RequestAttribute("currentUser") CurrentUser user) {
        return ticketService.list(user);
    }

    @PostMapping
    public Ticket create(@RequestAttribute("currentUser") CurrentUser user,
                         @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(user, request);
    }

    @PostMapping("/{id}/transition")
    public Map<String, Boolean> transition(@RequestAttribute("currentUser") CurrentUser user,
                                           @PathVariable long id, @Valid @RequestBody TransitionRequest request) {
        ticketService.transition(user, id, request);
        return Map.of("ok", true);
    }

    @GetMapping("/{id}/events")
    public List<TicketEvent> events(@RequestAttribute("currentUser") CurrentUser user, @PathVariable long id) {
        return ticketService.events(user, id);
    }
}
