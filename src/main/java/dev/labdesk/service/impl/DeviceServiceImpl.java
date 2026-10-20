package dev.labdesk.service.impl;

import dev.labdesk.entity.Device;
import dev.labdesk.mapper.DeviceMapper;
import dev.labdesk.service.DeviceService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeviceServiceImpl implements DeviceService {
    private final DeviceMapper deviceMapper;

    public DeviceServiceImpl(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<Device> list() {
        return deviceMapper.findAll();
    }
}
