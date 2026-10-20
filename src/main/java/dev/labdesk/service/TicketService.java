package dev.labdesk.service;

import dev.labdesk.dto.CreateTicketRequest;
import dev.labdesk.dto.TransitionRequest;
import dev.labdesk.entity.Ticket;
import dev.labdesk.entity.TicketEvent;
import dev.labdesk.security.CurrentUser;
import java.util.List;

public interface TicketService {
    List<Ticket> list(CurrentUser actor);
    Ticket create(CurrentUser actor, CreateTicketRequest request);
    void transition(CurrentUser actor, long id, TransitionRequest request);
    List<TicketEvent> events(CurrentUser actor, long id);
}
