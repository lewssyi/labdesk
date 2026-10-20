package dev.labdesk.service.impl;

import dev.labdesk.common.ApiError;
import dev.labdesk.entity.Device;
import dev.labdesk.mapper.BookingMapper;
import dev.labdesk.mapper.DeviceMapper;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.ReservationQueryService;
import dev.labdesk.vo.ReservationQueries.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ReservationQueryServiceImpl implements ReservationQueryService {
    private final DeviceMapper devices;
    private final BookingMapper bookings;

    public ReservationQueryServiceImpl(DeviceMapper devices, BookingMapper bookings) {
        this.devices = devices;
        this.bookings = bookings;
    }

    @Override
    public List<Device> searchDevices(String name) {
        String keyword = name == null ? "" : name.trim();
        ApiError.require(keyword.length() <= 100, 400, "设备名称最多100字符");
        return devices.findAll().stream().filter(d -> d.getName().contains(keyword)).toList();
    }

    @Override
    public Availability availability(long deviceId, String startText, String endText) {
        var start = parse(startText);
        var end = parse(endText);
        validateRange(start, end);
        ApiError.require(!start.isBefore(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))),
                400, "空闲查询开始时间必须在未来，请明确查询日期和时段");
        ApiError.require(deviceId > 0, 400, "设备编号必须为正整数");
        var device = devices.findById(deviceId);
        ApiError.require(device != null, 404, "设备不存在，请先查询设备列表");
        // 查询完整的重叠区间，不使用最近100条列表，也不返回其他用户信息。
        var occupied = new ArrayList<Slot>();
        for (var slot : bookings.findOccupiedSlots(deviceId, start, end)) {
            var clipped = new Slot(slot.start().isBefore(start) ? start : slot.start(),
                    slot.end().isAfter(end) ? end : slot.end());
            if (!occupied.isEmpty() && !clipped.start().isAfter(occupied.get(occupied.size() - 1).end())) {
                var prior = occupied.remove(occupied.size() - 1);
                occupied.add(new Slot(prior.start(), prior.end().isAfter(clipped.end()) ? prior.end() : clipped.end()));
            } else occupied.add(clipped);
        }
        var free = new ArrayList<Slot>();
        var cursor = start;
        if (device.isEnabled()) {
            for (var slot : occupied) {
                if (cursor.isBefore(slot.start())) free.add(new Slot(cursor, slot.start()));
                cursor = slot.end();
            }
            if (cursor.isBefore(end)) free.add(new Slot(cursor, end));
        }
        return new Availability(deviceId, device.getName(), device.isEnabled(), start, end,
                device.isEnabled() && occupied.isEmpty(), occupied, free,
                "北京时间；区间含开始、不含结束。查询不锁定名额，提交预约时需重新校验。" );
    }

    @Override
    public MyBookings myBookings(CurrentUser actor, String startText, String endText) {
        boolean noStart = startText == null || startText.isBlank();
        boolean noEnd = endText == null || endText.isBlank();
        ApiError.require(noStart == noEnd, 400, "预约时间筛选需要同时提供开始和结束时间");
        var start = noStart ? null : parse(startText);
        var end = noEnd ? null : parse(endText);
        if (start != null) validateRange(start, end);
        // 管理员也固定查本人，不复用管理员可查全量的 BookingService.list。
        var rows = bookings.findOwnInRange(actor.id(), start, end);
        return new MyBookings(rows.stream().limit(100).toList(), rows.size() > 100,
                "仅当前账号；最多100条，包含已取消记录，请根据status区分；时间为北京时间。" );
    }

    private LocalDateTime parse(String text) {
        try {
            var value = LocalDateTime.parse(text == null ? "" : text);
            ApiError.require(value.getNano() == 0, 400, "时间精确到秒，不接受小数秒");
            return value;
        } catch (DateTimeParseException e) {
            throw new ApiError(400, "时间须为北京时间 yyyy-MM-dd'T'HH:mm:ss，请明确日期和时段");
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        ApiError.require(start.isBefore(end), 400, "开始时间必须早于结束时间");
        ApiError.require(!end.isAfter(start.plusDays(7)), 400, "单次时间范围最多7天，请缩小范围");
    }
}
