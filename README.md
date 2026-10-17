# 停车与寻车管理系统

基于 [wuyan12lvv/parking-management-system](https://github.com/wuyan12lvv/parking-management-system) 的软件完善版本。保留 Vue 2 + Element UI、Spring Boot 和 Coze 代码节点结构，补全独立启动所需后端、服务端会话、车辆/订单持久化及运营报表。保留原 Apache-2.0 LICENSE；本分支的新增和修改可通过 Git 提交核对。

## 快速启动

需要 Java 17 或 21、Maven 3.9+、Node.js/npm。本机以 Node 24 构建；Vue CLI 旧间接依赖可能提示 engine/deprecation 警告。无需 MySQL/Coze 密钥即可启动：默认使用文件 H2 数据库，Coze 显示未配置。

终端一（项目根目录，PowerShell 7）：

```powershell
# 自行设置至少12字符的首次管理员密码；不会每次启动重置。
$env:ADMIN_PASSWORD = Read-Host '首次管理员密码（至少12字符）' -MaskInput
mvn -f AI-BOTSpringboot/pom.xml spring-boot:run
```

不支持 `-MaskInput` 的终端请通过系统/部署环境设置密码。Spring Boot 读取进程环境变量，不自动加载 `.env.backend.example`。

终端二（项目根目录）：

```powershell
npm ci
npm run serve
```

访问 `http://127.0.0.1:8080`，账号 `admin`，密码为刚设置的值。首次启动后创建普通用户，在运营页选择车主登记入场。普通用户也可自行注册，只能查询本人车辆/订单。

- 后端默认 `127.0.0.1:8090`，前端默认 `127.0.0.1:8080`。
- `ADMIN_PASSWORD` 仅在没有超级管理员时初始化。未设置则不创建管理员，服务仍可启动、注册普通用户。
- 初始化“校园演示停车场”100个车位，不伪造车辆、订单和营收；占用来自系统登记，每15秒刷新，并非硬件测量。
- 默认 H2 路径为运行工作目录下 `data/parking`。Maven 插件运行目录为后端模块；直接 `java -jar` 时相对于当前目录。部署请通过 `DB_URL` 指定固定绝对路径，避免换目录后误用新库。

## 构建与托管

```powershell
mvn -f AI-BOTSpringboot/pom.xml package
npm run build
# 终端一
java -Duser.timezone=Asia/Shanghai -jar AI-BOTSpringboot/target/parking-backend-0.1.0.jar
# 终端二
npm start
```

`server.js` 托管本地 `dist/`，代理 `/api`、`/user`、`/menu` 并为其他前端 history 路由回退 `index.html`。API 不存在时仍由后端返回错误，不伪装成首页。`dist/` 不再提交 Git，部署前必须 build。静态服务器可用 `API_TARGET`、`HOST`、`PORT` 修改目标及监听地址。

## 配置

复制 `.env.example` 为 `.env.local` 设置前端参数，修改后重启开发服务/重新构建：

| 变量 | 用途 |
|---|---|
| VUE_APP_API_BASE_URL | 浏览器 API 前缀，默认空，推荐同源代理 |
| DEV_API_TARGET | 开发代理后端，默认 http://127.0.0.1:8090 |
| API_TARGET | npm start 的服务端代理目标 |

PAT、数据库密码、高德服务端 Key 不得配置为 VUE_APP_*。跨域开发应使用相同主机名、允许源及 Cookie；正式部署优先同源代理。

后端变量见 `.env.backend.example`。默认文件 H2 使用 WRITE_DELAY=0，提交时立即写盘，重启保留所有业务记录。`schema.sql` 的 `CREATE TABLE IF NOT EXISTS` 只初始化新表，不清空已有数据；后续字段变更应增加迁移脚本。

MySQL 8：先创建空数据库和专用账号，设置 `DB_URL=jdbc:mysql://127.0.0.1:3306/parking?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`、`DB_USER`、`DB_PASSWORD`。新表为 `app_user/app_menu/parking_lot/vehicle_record/parking_order`，不是历史 Coze 数据库的自动迁移；已有数据须另行映射。本轮自动测试使用 H2 MySQL 模式，未执行真实 MySQL 迁移验证。

角色0超级管理员、1管理员、2普通用户。密码使用 BCrypt，响应不返回密码/哈希；匿名注册固定普通用户。运营、报表和用户列表要求管理员，普通用户限本人资料、车辆和订单。会话为服务端 JSESSIONID，已移除 httpBasic；先 GET `/api/csrf`，修改请求发送 `X-XSRF-TOKEN`，前端 Axios 封装自动处理。退出销毁服务端会话，刷新页面由 `/user/me` 恢复。菜单不是鉴权依据。

正式部署设置准确的 `ALLOWED_ORIGINS`、所需的 `SERVER_ADDRESS`；通过 HTTPS 提供服务，并设置 `SESSION_COOKIE_SECURE=true`。日期统计按服务器时区，建议 JVM `-Duser.timezone=Asia/Shanghai`。

## 接口

常规响应 `{code,msg,total,data}`，失败同时使用相应 HTTP 状态码。修改接口需要 CSRF。除健康检查、CSRF、登录、注册、账号存在性及独立鉴权的窄范围回调外均要求会话。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/health、/api/csrf | 健康检查、CSRF token |
| POST | /user/login | `{no,password}` 或 `{phone,password}`，返回 user/menu |
| POST | /user/logout | 退出登录 |
| GET | /user/me | 当前用户与菜单 |
| POST | /user/save | 公开注册/管理员新增，公开注册忽略角色输入 |
| POST | /user/update | 修改本人或有管理权限的用户，空密码不修改 |
| GET | /user/list | 管理员可管理的用户 |
| DELETE | /user/del?id=… | 删除用户，前端同步改为 DELETE；自己/更高权限/有业务关联的账号受保护 |
| GET | /user/findByNo?no=… | 仅存在性，不泄露资料；不存在沿用原 code=400 约定 |
| POST | /user/listPageC1 | `{pageNum,pageSize,param:{name,sex,roleId}}`，每页最多100条 |
| GET | /menu/list | 依据登录身份，忽略客户端 roleId |
| POST | /api/sendMessage | `{message}`，可选 Coze 智能体对话 |
| GET | /api/parking/lots | 停车场和登记占用/可用量 |
| GET | /api/parking/records、/api/parking/orders | 当前权限范围最近500条记录/订单；records可传active=true仅查在场车辆 |
| POST | /api/parking/entry | 管理员登记 `{lotId,userId,numberplate,spaceCode}`，时间由服务器记录 |
| POST | /api/parking/exit/{recordId} | 离场并生成唯一待收款订单，重复请求返回原订单 |
| POST | /api/parking/orders/{orderId}/paid | 人工确认实际已收款，重复请求不重复累计 |
| GET | /api/parking/overview | 今日入离场/实收与当前占用 |
| GET | /api/parking/reports?start=yyyy-MM-dd&end=yyyy-MM-dd | 逐日报表，含两端日期，最多367天 |
| POST | /api/parking/reverse | `{numberplate,now_location?}`，先检查归属，再查位置与可选 Coze 工作流 |
| POST | /api/parking/recommend | `{preference:"available"或"price"}`，本地停车场排序 |
| POST | /api/parking/route | 四个经纬度输入，调用 Coze 驾车路线工作流 |
| GET | /api/parking/integrations | 仅是否已配置，不返回密钥或 ID |

活跃车牌/车位有唯一约束，行锁保护容量检查，离场与订单在单个事务内提交；订单关联记录、车主和停车场。营收按实际收款日计算，待收款不计营收。人工确认只登记收款，不发起线上支付。当前是单个初始化场地原型，容量/费率可从数据库维护；未实现完整楼层编辑器、线上支付和硬件采集。

## 计费规则

免费分钟是宽限门槛，超过后从入场总时长计费，不扣除免费分钟。每开始一单位向上取整，首单位reg1，后续单位reg3，每24小时段封顶reg2。默认单位60分钟、免费15分钟、首小时5元、后续3元、24小时封顶20元。

示例：0秒→0，900秒→0，901秒→5，3601秒→8，86400秒→20，86401秒→25。Java 使用 BigDecimal，Python 使用 Decimal；负时长、负费率、零计费单位拒绝处理。无记录是明确错误，不使用100000或免费金额代替。

## Coze 工作流

工作流统一由后端 POST `https://api.coze.cn/v1/workflow/run`。浏览器不访问 Coze，不存 PAT。服务端环境变量：

| 变量 | 工作流契约 |
|---|---|
| COZE_API_TOKEN | 服务端 PAT |
| COZE_WF_CHARGE | 开始节点输入 car_number、numberplate；结束输出 `{price:数字}` |
| COZE_WF_REVERSE_SEARCH | car_number、now_location → `{message:[位置描述…]}` |
| COZE_WF_ROUTE_RECOMMEND | start_lat/start_lng/end_lat/end_lng → `{message:[{strategy,track},…]}` |
| COZE_BOT_ID | 仅独立 AI 对话页使用，后端 Coze v3 对话 API，与工作流无关 |

PAT+相应ID都有值才标记已配置；`data` 为 JSON 字符串时会解包。未配置时本地运营、计费、规则推荐和登记位置仍工作，显示本地来源；路线/AI问答提示未配置。已配置后失败不会静默伪装成功。聊天 user_id 由服务端随机生成，conversation_id 保存在登录会话中。

### 计费三节点

保持 `get_time → Get_Billing_information → Parking_charging_system`：开始节点声明 car_number、numberplate，通常映射同一车牌。

1. get_time：`car_number → time` 秒。
2. Get_Billing_information：`numberplate → reg1/reg2/reg3/unit/free_time`。
3. 将 time 映射为最后节点的 total_seconds，费率直接映射，结束输出数值 price。

前两节点改为从本系统后端读取，避免继续查询与新车辆记录脱节的旧数据库。Coze 运行环境安全注入 `PARKING_API_URL`（云端可达后端 HTTPS 域名）及 `COZE_SERVICE_TOKEN`（至少32字符，与后端相同）。回调为 GET `/api/integration/time?car_number=…`、`/api/integration/billing?numberplate=…`，使用 `Authorization: Bearer <COZE_SERVICE_TOKEN>`。这仅是窄范围只读回调凭据，不是管理员会话。

如果代码节点版本不支持安全的环境变量/Secret 注入，替换为平台支持的 HTTP 工具节点，使用相同接口和受保护请求头；不要把密钥写进源码。默认服务仅监听本机，云端 Coze 无法访问127.0.0.1，需部署团队控制的可达 HTTPS 服务后联调。

Coze 费用必须与本地数据库费率计算一致，才原子生成订单和离场；调用失败/缺数值price/金额不一致则回滚，车辆仍在场。计费门槛附近节点读取时刻不同可能触发不一致，重试即可，系统不会接受错误费用。

### 反向寻车、驾车路线和旧节点

反向寻车保留 car_number、now_location → message列表。旧节点仍查询历史 parking_car.camera_location；若继续使用，需在Coze设置 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD 并确保旧位置数据与新系统同步。本地前端始终展示本系统登记车位，不把登记车位宣称为摄像头识别位置。

track_recommend1~4 是高德驾车路线，不是空车位推荐。可将四节点聚合为一个发布工作流，按上述四个经纬度输入、message列表输出，也可只发布一个策略。本地 /recommend 另以空位或首小时价格排序。

location.py 和路线节点的高德 Key 改为 `AMAP_API_KEY`，旧MySQL节点凭据全部外置。location.py 输出拼写 **tatol** 保持兼容，映射时不要误写成total。

仓库仅有代码节点导出，不含完整工作流图。需在平台替换节点、连接字段、发布支持API调用的版本，再注入ID。本轮未登录或修改云端Coze，未用真实PAT在线联调；适配器有模拟协议测试，后续注入环境变量无需改后端代码。

## 验证

```powershell
mvn -f AI-BOTSpringboot/pom.xml test
npm run lint
npm run build
python -m unittest discover -s tests -v
# 已完成 package/build 后，启动实际服务并验证重启持久化
python scripts/smoke.py
```

Java测试覆盖会话/CSRF/权限、用户CRUD、车辆归属、重复入场、离场订单幂等、人工收款统计、计费边界、未配置提示、Coze字段与JSON解包、错误费用回滚和正确费用落库。Python离线验证计费边界与节点语法，不依赖Coze运行时。上线前另行验证真实MySQL、已发布Coze工作流、回调网络、实际场地费率及终端显示效果，不将模拟协议测试表述为真实Coze联调。
