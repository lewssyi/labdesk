package dev.labdesk.controller;

import dev.labdesk.entity.Device;
import dev.labdesk.service.DeviceService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<Device> list() {
        return deviceService.list();
    }
}
