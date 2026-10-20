package dev.labdesk.service.impl;

import dev.labdesk.common.ApiError;
import dev.labdesk.dto.ReserveRequest;
import dev.labdesk.entity.Booking;
import dev.labdesk.mapper.BookingMapper;
import dev.labdesk.mapper.DeviceMapper;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.BookingService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingMapper bookingMapper;
    private final DeviceMapper deviceMapper;

    public BookingServiceImpl(BookingMapper bookingMapper, DeviceMapper deviceMapper) {
        this.bookingMapper = bookingMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<Booking> list(CurrentUser actor) {
        return actor.isAdmin() ? bookingMapper.findAllRecent() : bookingMapper.findRecentByUser(actor.id());
    }

    @Override
    @Transactional
    public Booking reserve(CurrentUser actor, ReserveRequest request, String requestKey) {
        LocalDateTime start = request.start();
        LocalDateTime end = request.end();
        ApiError.require(start != null && end != null && start.isBefore(end)
                && start.isAfter(LocalDateTime.now()), 400, "开始时间须在未来且早于结束时间");
        ApiError.require(requestKey != null && requestKey.length() <= 64 && !requestKey.isBlank(),
                400, "需要 1~64 字符的 Idempotency-Key");
        ApiError.require(start.getNano() == 0 && end.getNano() == 0, 400, "预约时间精确到秒，不接受小数秒");

        // Mapper 执行 FOR UPDATE；Service 的事务覆盖后面的检查和插入。
        var device = deviceMapper.findByIdForUpdate(request.deviceId());
        ApiError.require(device != null && device.isEnabled(), 404, "设备不存在或不可用");

        // 先识别旧请求，避免重试与自己的预约发生冲突。
        var prior = bookingMapper.findByRequestKey(actor.id(), requestKey);
        if (prior != null) {
            boolean same = prior.getDeviceId() == request.deviceId()
                    && start.equals(prior.getStartAt()) && end.equals(prior.getEndAt());
            ApiError.require(same, 409, "同一个幂等标识不能用于不同预约");
            return prior;
        }

        int conflicts = bookingMapper.countOverlaps(request.deviceId(), start, end);
        ApiError.require(conflicts == 0, 409, "该设备在此时段已有预约");
        var booking = new Booking();
        booking.setUserId(actor.id());
        booking.setDeviceId(request.deviceId());
        booking.setStartAt(start);
        booking.setEndAt(end);
        booking.setRequestKey(requestKey);
        bookingMapper.insert(booking);
        return bookingMapper.findById(booking.getId());
    }

    @Override
    @Transactional
    public void cancel(CurrentUser actor, long id, int version) {
        var booking = bookingMapper.findById(id);
        ApiError.require(booking != null, 404, "预约不存在");
        ApiError.require(actor.isAdmin() || booking.getUserId() == actor.id(), 403, "无权取消此预约");
        int changed = bookingMapper.cancelIfVersionMatches(id, version);
        ApiError.require(changed == 1, 409, "预约已变化，请刷新后重试");
    }
}
