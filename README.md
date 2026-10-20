# LabDesk：实验室设备预约与故障工单

面向 Java 后端实习的单模块学习项目。业务与你的实验室经历接近，从一条预约请求开始读代码即可，不需要先理解 yudao 的多模块体系。

技术：Java 17+、Spring Boot 3、Spring MVC、MyBatis、MySQL、Redis、Bean Validation。前端只有一个静态 HTML，无需安装或启动 Vue。代码严格按 Controller → Service → Mapper 分层，数据库 SQL 集中在 Mapper XML。

## 三层架构

```text
src/main/java/dev/labdesk/
├── controller/       HTTP 接口，接收参数、调用 Service、返回结果
├── service/          业务接口
│   └── impl/         权限、状态、冲突、幂等与事务
├── mapper/           MyBatis 数据访问接口；SessionMapper 封装 Redis
├── entity/           数据库实体
├── dto/              请求参数及 Bean Validation
├── vo/               登录、知识问答的响应对象
├── security/         认证拦截器与可信用户身份
├── config/           Web 配置、演示数据初始化入口
├── integration/      外部模型 HTTP 适配器
└── common/           业务异常与全局错误处理
src/main/resources/mapper/  与 MyBatis Mapper 一一对应的 SQL XML
```

调用路径以预约为例：`BookingController.reserve()` → `BookingService` → `BookingServiceImpl.reserve()` → `DeviceMapper/BookingMapper` → `DeviceMapper.xml/BookingMapper.xml` → MySQL。

- Controller 只注入 Service 接口，不使用 Mapper、JdbcTemplate、Redis 或事务。
- ServiceImpl 组织业务规则并调用 Mapper，写操作的 `@Transactional` 放在这里，不直接写 SQL。
- Mapper 只做数据访问，不判断学生/管理员权限，也不决定工单是否允许流转。
- SQL 使用 `#{参数}` 绑定，不拼接请求文本。插入主键通过 MyBatis `useGeneratedKeys` 回填。
- 认证拦截器和初始化入口同样调用 Service，不绕过业务层。
- Redis 会话由 `SessionMapper` 封装，模型网络请求由 `integration` 适配器封装；是否检索、调用模型或降级仍由 Service 决定。

使用 MyBatis Starter 3.0.5 与现有 Spring Boot 3.5。可参考[官方兼容说明](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)和[3.0.5 发布说明](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-3.0.5)。

## 数据库配置

数据库密码通过环境变量 `LAB_DB_PASSWORD` 设置，也可以在项目根目录新建 `application-local.yaml`：

```yaml
spring:
  datasource:
    password: "你的本机数据库密码"
```

该本地配置已加入 `.gitignore`，不会上传到仓库。数据库用户名可通过 `LAB_DB_USER` 设置，模型密钥通过 `DASHSCOPE_API_KEY` 或 `LAB_AI_KEY` 设置。

## 在这台电脑上运行

- 项目：`E:\Java\labdesk`。
- 双击 `start.cmd`，浏览器访问 http://127.0.0.1:18080/。
- 学生 `student / student123`；管理员 `admin / admin123`。
- 双击 `stop.cmd` 只停止 LabDesk。MySQL、Redis是两个项目共用的本机开发服务。
- 改代码后：先停止，再在 PowerShell 执行 `E:\Java\labdesk\start.ps1 -Rebuild`。
- 后端 JVM 最大堆设为 512 MB；这不是总进程内存上限，还要计入 JVM 非堆、MySQL、Redis、IDEA。无需服务器即可学习演示。

启动脚本使用本机已安装的 Java、IDEA Maven、隔离 MySQL 3307 和 Redis 6379，路径在脚本中明确列出。项目代码不依赖 yudao。没有 Node、微服务、消息队列或本地大模型。

## IDEA 导入

1. File → Open，选择 `E:\Java\labdesk\pom.xml`，以新项目打开。
2. Project SDK 选择已有的 `temurin-21`，Maven Reload。
3. 运行 `LabdeskApplication.main()`，或选择共享配置 `LabDesk`。使用 IDEA 启动前先停止脚本启动的 18080 进程。
4. 如果依赖下载失败，Maven User settings 选择 `dev\maven-settings.xml`。该配置使用本机 7897 代理；代理未开启或迁移电脑时改用 Maven 默认设置。

## 四条核心流程

| 流程 | 实现 | 面试需要解释的问题 |
| --- | --- | --- |
| 登录 | BCrypt 校验、Redis 保存随机 token，2 小时 TTL | 为什么密码不明文存？退出如何让 token 失效？ |
| 预约 | 事务内锁设备行，检查时间区间，再插入 | 为什么单纯“先查后插”会产生并发超订？锁的粒度是多少？ |
| 重复提交 | 用户+请求标识唯一索引，校验同一标识的请求参数 | 幂等和普通时间冲突检查有什么区别？ |
| 工单 | 明确状态链、角色权限、version 条件更新、事务日志 | 乐观锁如何阻止旧请求覆盖结果？失败时日志会留下吗？ |

工单状态：`OPEN → ACCEPTED → PROCESSING → RESOLVED → CLOSED`。管理员处理；学生只能确认关闭已经解决的本人工单。用户只看到自己的预约和工单，管理员可查看全部。列表目前只返回最近 100 条，并非正式分页。

预约使用半开区间 `[开始,结束)`，可首尾相接。第一版按设备串行处理预约，适合小规模场景；并未实现分布式锁。幂等标识长期保存在预约行中，同一标识取消后仍代表原请求，重新预约请使用新标识。

## 代码阅读顺序

1. `src/main/resources/schema.sql`：先理解 6 张表、索引、version 和 request_key。
2. `controller/BookingController.java`、`dto/ReserveRequest.java`：看接口输入、参数校验和 Service 调用。
3. `service/BookingService.java` → `service/impl/BookingServiceImpl.java`：读业务接口及预约事务、幂等。
4. `mapper/BookingMapper.java`、`resources/mapper/BookingMapper.xml`：逐项对应 Java 方法和 SQL；设备行锁在 `DeviceMapper.xml`。
5. `controller/TicketController.java` → `service/impl/TicketServiceImpl.java` → 工单 Mapper：读权限、乐观锁、事务日志。
6. `security/AuthInterceptor.java` → `service/impl/AuthServiceImpl.java` → `UserMapper/SessionMapper`：读认证和 Redis 会话；错误处理在 `common/GlobalExceptionHandler.java`。
7. `service/impl/KnowledgeServiceImpl.java`：最后学习关键词检索、模型调用和降级，SQL 在 `KnowledgeMapper.xml`，HTTP 在 `integration/DeepSeekChatModelClient.java`。

## 4 周学习与改造

以下按每周约 10～15 小时安排，掌握程度优先于赶进度。

| 时间 | 学习内容 | 必须亲手完成的结果 |
| --- | --- | --- |
| 第 1 周 | HTTP、MVC、依赖注入、SQL CRUD、参数校验 | 不看现成代码实现设备新增/修改接口，并给管理员授权 |
| 第 2 周 | MySQL 索引、事务、行锁、时间区间、幂等 | 用自己的话讲明预约流程；添加预约时长限制和时间段筛选 |
| 第 3 周 | Redis TTL、认证、乐观锁、状态流转 | 添加工单优先级、负责人、列表分页，并补上相应验收 |
| 第 4 周 | API 调用、知识检索、超时降级、结果评估 | 接真实模型，制作 20～30 个问答样本，记录有依据回答率与失败案例 |

先做完 Java 业务，再加 AI。完成第 3 周的独立改造、能现场演示并讲清设计后，可以作为个人学习项目投递 Java 实习；真实模型接通并评估后再加 AI 描述。

## 实验室预约助手（Spring AI Alibaba）

页面新增“实验室预约助手”，支持自然语言查设备、查未来空闲时段、查本人预约。后端使用 `spring-ai-alibaba-dashscope:1.1.2.0` 和原生 DashScope 接口，配合项目内有次数上限的工具调用循环。当前不使用 ReactAgent、多 Agent 或向量 RAG。原来的业务接口和知识库助手继续保留。

### 配置与启动

在百炼控制台创建 API Key，在同一个 PowerShell 窗口中执行：

```powershell
cd E:\Java\labdesk
$env:DASHSCOPE_API_KEY = "替换成你的百炼API Key"
$env:LAB_AGENT_MODEL = "qwen-plus"  # 可省略，默认 qwen-plus
.\stop.ps1
.\start.ps1
```

修改源码后用 `.\start.ps1 -Rebuild`。构建脚本在检测到本机7897代理时使用 `dev/maven-settings.xml`，否则使用 Maven 默认设置。密钥不要写入源码或提交仓库；上述环境变量仅在当前会话及其子进程生效。从 IDEA 启动时，在运行配置的环境变量中设置 `DASHSCOPE_API_KEY`。

默认 `LAB_AGENT_BASE_URL=https://dashscope.aliyuncs.com`，使用 DashScope 原生接口而非 OpenAI 兼容路径；如更换地域，需使用对应地域的服务地址、密钥和可用模型。配置项映射在 `application.yaml` 的 `spring.ai.dashscope` 下。旧知识库的 `LAB_AI_KEY` 与预约助手的密钥配置独立。

打开 http://127.0.0.1:18080/ 并登录，可依次尝试：

- “有哪些设备？”
- “示波器明天14:00到16:00可以预约吗？”
- “查一下我本周的预约。”
- 时间或设备含糊时，继续补充具体日期、起止时间或设备编号。

页面展示回答和可展开的工具执行结果，只在当前页面保留最近3轮上下文；清空、退出或切换账号时清除上下文。每次查询都重新读取实时数据。未配置密钥时显示明确提示，不生成模拟回答。

### 三个工具及调用链

| 工具 | 功能 | 数据范围 |
| --- | --- | --- |
| `search_devices(name)` | 按名称查设备，返回编号和启用状态 | 设备列表 |
| `query_availability(deviceId,start,end)` | 返回占用区间、空闲区间及完整时段是否可预约 | 未来最多7天；不返回其他预约人信息 |
| `list_my_bookings(start,end)` | 查询本人预约，支持最多7天的时间筛选 | 固定当前登录账号，管理员同样只查本人；最多100条，截断时标记 |

`AgentController` → `AgentServiceImpl` → `DashScopeAgentModelClient` / `ReservationTools` → `ReservationQueryServiceImpl` → Mapper / XML。

`AgentServiceImpl` 每次请求创建绑定可信身份的工具对象，身份不暴露为模型参数；工具直接调用 Service，不绕过业务层。数据库查询结果只给模型必要字段，不返回用户编号、幂等标识。时间使用北京时间；预约和空闲区间均为半开区间 `[开始,结束)`，取消记录不占用设备，禁用设备无可预约时段。空闲查询使用完整重叠区间 SQL，而非最近100条预约列表。

接口均需登录：

```text
GET  /api/agent/status
POST /api/agent/chat
Authorization: Bearer <登录token>

{"message":"示波器明天14:00到16:00可以预约吗？","history":[]}
```

`history` 可省略，仅接受最多6条 `user` / `assistant` 消息。响应包含 `mode`、`answer`、`tools`（工具名、是否成功、结果）；模式为 `AGENT`、`NOT_CONFIGURED`、`UNAVAILABLE` 或 `LIMIT_REACHED`。最多4轮模型请求、8次工具调用，单次HTTP读取超时20秒，连接超时5秒，不自动重试模型请求。工具参数由后端校验，未知工具不会执行。

当前助手只查询，不能创建、取消预约。查询结果不锁定名额，用户仍须通过预约表单提交，由原有事务、设备行锁和幂等校验决定是否成功。模型对自然语言的理解和回答质量需要真实密钥验收；本地协议替身测试不能替代真实模型评估。

依赖依据：[版本兼容表](https://java2ai.com/docs/versions/)、[DashScope手动配置](https://java2ai.com/integration/chatmodels/dashScope/)。

## 原有知识库助手

关键词匹配最多3条资料，返回来源编号；未命中时提示资料不足，未配置密钥时显示仅检索。原有 DeepSeek HTTP 适配器带超时和失败降级，通过 `LAB_AI_KEY`、可选 `LAB_AI_ENDPOINT` / `LAB_AI_MODEL` 配置，尚未验证真实模型调用。它不属于新预约 Agent 的工具。

尚未实现文件解析、分块、embedding、向量数据库或语义排序，不应描述为完整向量 RAG。官方接口文档：https://api-docs.deepseek.com/api/create-chat-completion/

## 验证

`mvn test` 执行 Agent 和预约查询单元测试，验证空闲区间、参数校验、本人权限、并发身份隔离、工具执行循环和次数限制；不需要数据库或模型密钥。

后端与本机 MySQL/Redis 已启动、JAR 已重新构建时，执行 `python tests\agent_smoke.py`。脚本启动独立测试后端和本地 DashScope 原生协议替身，验证三个工具从 HTTP 到真实数据库的链路、半开区间、取消释放、学生及管理员本人范围、请求校验与模型错误降级；退出时停止自己启动的进程，保留正常后端。报告保存在 `tests\agent-result.json`，测试应用日志在 `target\agent-smoke-runtime.log`。该测试不消耗模型额度，也不证明真实模型能正确理解问题。

后端运行时执行 `python tests\smoke.py`，结果保存到 `tests\last-result.json`。脚本在真实 MySQL/Redis 上验证权限、预约幂等、相邻区间、取消释放、工单状态与日志、知识库模式、退出失效。

执行 `python tests\architecture.py` 检查源码依赖：Controller 不能调用 Mapper、数据库/Redis API 只能在 Mapper 使用、SQL 不能出现在业务层、Service 接口与实现对应、XML 命名空间与方法一致。结果保存到 `tests\architecture-result.json`。这是源码结构检查，不替代业务验收。

两个并发案例各发出 20 个请求，预期分别只有 1 次预约或状态更新成功，其余返回 409。此测试验证正确性，不代表高并发吞吐量。脚本会留下已取消的预约和已关闭、带“自动验收”标题的工单，不删除已有业务数据。

## 简历怎么写

真正理解并完成独立改造后，可根据实际完成范围使用：

**LabDesk 实验室设备预约与故障工单系统｜个人学习项目**

- 基于 Spring Boot、MyBatis、MySQL、Redis，按 Controller/Service/Mapper 三层实现设备预约、故障工单和角色权限，使用 BCrypt 与 Redis 会话实现登录和退出。
- 通过事务与设备行锁处理重叠预约，结合请求标识及唯一索引实现重复请求识别；采用 version 条件更新和事务日志保证工单流转一致性。
- 完成预约和工单的并发正确性验证，分别在 20 个并发请求中验证只有一次有效写入。

接通模型并完成评估后，才补充 AI 条目及真实评估结果。不要将助手生成的初始实现直接声称为全部独立设计；你自己的新增功能、设计取舍与调试过程，才是项目答辩的重点。不得虚构上线用户、企业实习经历、QPS 或性能提升比例。

## 迁移到别的电脑

安装 Java 17+、Maven、MySQL 8 和 Redis；创建 `labdesk` 数据库并给专用账号授权。通过 `LAB_DB_USER`、`LAB_DB_PASSWORD` 覆盖本机默认开发账号，必要时修改 application.yaml 的连接地址；执行 `mvn package` 后 `java -jar target/labdesk-0.1.0.jar`。表和演示数据首次启动自动创建，已有记录不会被重置。本仓库附带的 Windows 启动脚本只面向这台电脑的安装路径。
