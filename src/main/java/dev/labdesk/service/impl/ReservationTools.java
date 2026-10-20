package dev.labdesk.service.impl;

import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.ReservationQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** 每次请求独立创建，可信身份不属于模型参数，避免并发请求串用身份。 */
public class ReservationTools {
    private final ReservationQueryService queries;
    private final CurrentUser actor;

    public ReservationTools(ReservationQueryService queries, CurrentUser actor) {
        this.queries = queries;
        this.actor = actor;
    }

    @Tool(name = "search_devices", description = "根据名称查找实验室设备，返回真实编号、名称、启用状态；空名称查询所有设备。名称匹配多台时请用户选择，禁止猜测设备编号。")
    public Object searchDevices(@ToolParam(description = "设备名称关键词，可为空字符串") String name) {
        return queries.searchDevices(name);
    }

    @Tool(name = "query_availability", description = "查询具体设备在未来最多7天范围内的占用区间、空闲区间、整个时段是否可预约。先用search_devices确定设备。查询不会创建预约。")
    public Object availability(@ToolParam(description = "查询得到的真实设备编号") long deviceId,
            @ToolParam(description = "北京时间，格式yyyy-MM-dd'T'HH:mm:ss") String start,
            @ToolParam(description = "北京时间，格式yyyy-MM-dd'T'HH:mm:ss，结束晚于开始") String end) {
        return queries.availability(deviceId, start, end);
    }

    @Tool(name = "list_my_bookings", description = "仅查询当前登录用户的预约，包括已取消状态；管理员也仅查询本人。时间筛选最多7天，无筛选时返回最近100条。无法查询其他用户。")
    public Object myBookings(
            @ToolParam(required = false, description = "可选，北京时间yyyy-MM-dd'T'HH:mm:ss，与end同时提供") String start,
            @ToolParam(required = false, description = "可选，北京时间yyyy-MM-dd'T'HH:mm:ss，与start同时提供") String end) {
        return queries.myBookings(actor, start, end);
    }
}
