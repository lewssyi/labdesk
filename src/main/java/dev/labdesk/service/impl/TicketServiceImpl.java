package dev.labdesk.service.impl;

import dev.labdesk.common.ApiError;
import dev.labdesk.dto.CreateTicketRequest;
import dev.labdesk.dto.TransitionRequest;
import dev.labdesk.entity.Ticket;
import dev.labdesk.entity.TicketEvent;
import dev.labdesk.mapper.DeviceMapper;
import dev.labdesk.mapper.TicketMapper;
import dev.labdesk.mapper.TicketEventMapper;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.TicketService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {
    private static final Map<String, String> NEXT = Map.of(
            "OPEN", "ACCEPTED", "ACCEPTED", "PROCESSING",
            "PROCESSING", "RESOLVED", "RESOLVED", "CLOSED");
    private final TicketMapper ticketMapper;
    private final TicketEventMapper eventMapper;
    private final DeviceMapper deviceMapper;

    public TicketServiceImpl(TicketMapper ticketMapper, TicketEventMapper eventMapper, DeviceMapper deviceMapper) {
        this.ticketMapper = ticketMapper;
        this.eventMapper = eventMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<Ticket> list(CurrentUser actor) {
        return actor.isAdmin() ? ticketMapper.findAllRecent() : ticketMapper.findRecentByUser(actor.id());
    }

    @Override
    @Transactional
    public Ticket create(CurrentUser actor, CreateTicketRequest request) {
        ApiError.require(deviceMapper.findById(request.deviceId()) != null, 404, "设备不存在");
        var ticket = new Ticket();
        ticket.setUserId(actor.id());
        ticket.setDeviceId(request.deviceId());
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        // MyBatis 将数据库生成的主键回填到实体，随后用它写日志。
        ticketMapper.insert(ticket);
        eventMapper.insert(ticket.getId(), actor.id(), "", "OPEN", "创建工单");
        return ticketMapper.findById(ticket.getId());
    }

    @Override
    @Transactional
    public void transition(CurrentUser actor, long id, TransitionRequest request) {
        var ticket = ticketMapper.findById(id);
        ApiError.require(ticket != null, 404, "工单不存在");
        String from = ticket.getStatus();
        String to = request.status();
        ApiError.require(to.equals(NEXT.get(from)), 409, "不允许从 " + from + " 直接变为 " + to);
        boolean owner = ticket.getUserId() == actor.id();
        ApiError.require(actor.isAdmin() || (owner && "CLOSED".equals(to)), 403, "当前账号不能执行此操作");

        // 状态条件和版本条件一起匹配，防止旧请求覆盖新处理结果。
        int changed = ticketMapper.updateStatusIfVersionMatches(id, request.version(), from, to);
        ApiError.require(changed == 1, 409, "其他人已处理此工单，请刷新后重试");
        // 日志和状态更新属于同一 Service 事务。
        eventMapper.insert(id, actor.id(), from, to, request.note());
    }

    @Override
    public List<TicketEvent> events(CurrentUser actor, long id) {
        var ticket = ticketMapper.findById(id);
        ApiError.require(ticket != null && (actor.isAdmin() || ticket.getUserId() == actor.id()),
                404, "工单不存在或无权查看");
        return eventMapper.findByTicketId(id);
    }
}
