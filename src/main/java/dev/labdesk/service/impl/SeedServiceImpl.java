package dev.labdesk.service.impl;

import dev.labdesk.entity.UserAccount;
import dev.labdesk.mapper.UserMapper;
import dev.labdesk.mapper.DeviceMapper;
import dev.labdesk.mapper.KnowledgeMapper;
import dev.labdesk.service.SeedService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedServiceImpl implements SeedService {
    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper;
    private final KnowledgeMapper knowledgeMapper;

    public SeedServiceImpl(UserMapper userMapper, DeviceMapper deviceMapper, KnowledgeMapper knowledgeMapper) {
        this.userMapper = userMapper;
        this.deviceMapper = deviceMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    @Override
    @Transactional
    public void initialize() {
        seedUser(1, "admin", "admin123", "ADMIN");
        seedUser(2, "student", "student123", "USER");
        deviceMapper.insertIfAbsent(1, "实验工作站");
        deviceMapper.insertIfAbsent(2, "示波器");
        deviceMapper.insertIfAbsent(3, "GPU 服务器");
        knowledgeMapper.insertIfAbsent(1, "预约规则", "预约,时间,冲突",
                "预约按半开区间计算：结束时间等于下一次开始时间时不冲突。取消后时段释放。");
        knowledgeMapper.insertIfAbsent(2, "设备无法开机", "开机,电源,黑屏",
                "先核对供电和设备电源指示灯。请勿自行拆机；检查无果后创建故障工单。");
        knowledgeMapper.insertIfAbsent(3, "GPU 任务异常", "GPU,显存,任务",
                "记录任务编号、报错信息和资源需求，检查资源占用；持续异常请提交管理员处理。");
    }

    private void seedUser(long id, String username, String password, String role) {
        var user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setRole(role);
        userMapper.insertIfAbsent(user);
    }
}
