# 飞机飞行传感器数据回溯系统

基于 Spring Boot + PostgreSQL + Vue 3 + ECharts 构建的飞机飞行传感器数据管理与回溯系统。

---

## 系统特性

- **大文件上传**: 支持几百MB级别的 CSV 日志文件上传，实时显示上传进度
- **异步解析入库**: 后端异步流式解析CSV，批量插入数据库，进度可追踪
- **多维度图表展示**: 使用 ECharts 展示高度、速度、发动机温度、燃油消耗等多维数据的历史趋势
- **综合/分图视图切换**: 支持多维叠加折线图和单指标分图两种查看模式
- **数据采样**: 支持大数据量下自动降采样，保证图表渲染性能
- **航班管理**: 航班列表、搜索、删除功能

---

## 项目结构

```
cn6/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/flight/sensor/
│       ├── FlightSensorApplication.java   # 主启动类
│       ├── config/
│       │   └── CorsConfig.java            # 跨域配置
│       ├── controller/
│       │   ├── UploadController.java      # 文件上传接口
│       │   ├── FlightController.java      # 航班&数据查询接口
│       │   └── GlobalExceptionHandler.java# 全局异常处理
│       ├── entity/                        # 实体类
│       │   ├── Flight.java                # 航班
│       │   ├── SensorData.java            # 传感器数据
│       │   └── UploadTask.java            # 上传任务
│       ├── repository/                    # JPA Repository
│       ├── service/                       # 业务逻辑
│       │   ├── FlightService.java
│       │   ├── SensorDataService.java
│       │   └── UploadService.java         # 大文件上传&CSV解析核心
│       └── dto/                           # 数据传输对象
│
├── frontend/               # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js
│       ├── api/index.js                # 后端API封装
│       ├── styles/global.scss          # 全局样式
│       └── views/
│           ├── UploadPage.vue          # 文件上传页（带进度条）
│           ├── HistoryPage.vue         # 数据回溯页（多维折线图）
│           └── FlightListPage.vue      # 航班列表页
│
└── scripts/                # 辅助脚本
    ├── init_db.sh                   # 数据库初始化
    └── generate_sample_data.py      # 生成模拟CSV测试数据
```

---

## 一、环境准备

### 必需软件

| 软件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| PostgreSQL | 13+ |

---

## 二、数据库配置

### 1. 创建数据库

```bash
# 方法一: 使用脚本（需要配置好psql环境）
bash scripts/init_db.sh

# 方法二: 手动执行
psql -U postgres -c "CREATE DATABASE flight_sensor;"
```

### 2. 修改数据库连接

如需修改默认用户名/密码，请编辑：
[application.yml](backend/src/main/resources/application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flight_sensor
    username: postgres
    password: postgres
```

> JPA `ddl-auto: update` 会自动创建表结构，无需手动建表

---

## 三、启动后端

```bash
cd backend

# 方式一: Maven 直接运行
mvn spring-boot:run

# 方式二: 打包后运行
mvn clean package -DskipTests
java -jar target/flight-sensor-backend-1.0.0.jar
```

启动后访问: http://localhost:8080

后端 API 端口：`8080`

### 后端核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/upload/file` | 上传CSV文件（multipart/form-data） |
| GET  | `/api/upload/progress/{taskId}` | 查询解析进度 |
| GET  | `/api/upload/tasks` | 上传任务列表 |
| GET  | `/api/flights` | 航班列表（支持keyword搜索） |
| GET  | `/api/flights/numbers` | 获取所有航班号（自动补全用） |
| GET  | `/api/flights/{flightNumber}` | 航班基本信息 |
| GET  | `/api/flights/{flightNumber}/data` | 传感器数据（支持maxPoints采样、startTime/endTime时间范围） |
| DELETE | `/api/flights/{id}` | 删除航班及数据 |

---

## 四、启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

启动后访问: http://localhost:5173

### 生产环境构建

```bash
npm run build
# 产物在 dist/ 目录
```

---

## 五、CSV 文件格式说明

CSV 表头支持**中英文别名**，大小写不敏感，支持下划线/空格/连字符分隔。

| 字段 | 必须 | 支持的表头名称 | 说明 |
|------|------|--------------|------|
| **timestamp** | ✅ | `timestamp`, `time`, `datetime`, `date`, `时间` | 支持多种时间格式或毫秒时间戳 |
| altitude | - | `altitude`, `height`, `alt`, `高度` | 高度（米） |
| speed | - | `speed`, `velocity`, `tas`, `ias`, `速度` | 速度（km/h） |
| engineTemperature | - | `engineTemperature`, `egt`, `engTemp`, `temp`, `temperature`, `发动机温度` | 发动机温度（℃） |
| fuelConsumption | - | `fuelConsumption`, `fuelFlow`, `fuel`, `ff`, `燃油消耗` | 燃油消耗 |
| cabinPressure | - | `cabinPressure`, `pressure`, `客舱压力` | 客舱压力 |
| latitude | - | `latitude`, `lat`, `纬度` | 纬度 |
| longitude | - | `longitude`, `lon`, `lng`, `经度` | 经度 |
| verticalSpeed | - | `verticalSpeed`, `vs`, `垂直速度`, `vy` | 垂直速度 |
| heading | - | `heading`, `hdg`, `航向` | 航向角 |

### 时间格式支持

- `YYYY-MM-DD HH:mm:ss`
- `YYYY-MM-DDTHH:mm:ss`
- `YYYY/MM/DD HH:mm:ss`
- `YYYY-MM-DD HH:mm:ss.SSS`
- 毫秒级 Unix 时间戳

### 示例 CSV

```csv
timestamp,altitude,speed,engineTemperature,fuelConsumption,cabinPressure,latitude,longitude,verticalSpeed,heading
2026-06-18 08:00:00,0.00,200.00,500.00,3.2000,101.33,39.9082,116.4074,12.00,120.5
2026-06-18 08:00:01,15.20,212.33,510.50,3.3100,101.25,39.9083,116.4075,13.50,121.2
...
```

---

## 六、生成测试数据

使用 Python 脚本生成模拟飞行数据：

```bash
cd scripts

# 生成 2 小时航班 CA1234，采样间隔 1 秒（约7200条，~600KB）
python3 generate_sample_data.py -f CA1234 -H 2 -i 1

# 生成 5 小时航班 MU5678，采样间隔 1 秒（约18000条，~1.5MB）
python3 generate_sample_data.py -f MU5678 -H 5 -i 1

# 生成 2 小时航班，采样间隔 100ms（约72000条，~6MB）
python3 generate_sample_data.py -f CZ9012 -H 2 -i 0.1

# 生成超大文件 - 10小时，1秒间隔（约36000条，实际如需大文件可减小间隔）
python3 generate_sample_data.py -f AA9988 -H 10 -i 1 -o large_flight.csv
```

> 想生成**几百MB**的测试文件，可以：
> ```bash
> # 50小时, 50ms 间隔 = ~3,600,000 条记录 ≈ 250MB
> python3 generate_sample_data.py -f BIG001 -H 50 -i 0.05
> ```

---

## 七、使用流程

### 1. 数据上传

1. 进入"数据上传"页面
2. 输入航班号（如 CA1234）
3. 拖拽或选择 CSV 文件
4. 点击"开始上传并解析"
5. 等待上传进度条和解析进度完成
6. 完成后可点击记录中的"查看数据"

### 2. 数据回溯

1. 进入"数据回溯"页面
2. 输入或选择航班号
3. 选择数据采样率（数据量大时建议 500 点或更少）
4. 切换综合视图 / 分图视图
   - **综合视图**: 多维叠加在一张图，可在图例区勾选要显示的指标
   - **分图视图**: 每个指标一张独立折线图，带最小/平均/最大统计
5. 图表支持：
   - 区域缩放（底部滑块 + 鼠标滚轮）
   - Hover 查看各点数据

### 3. 航班管理

- 在"航班列表"页可搜索、删除航班
- 删除航班会同时删除其所有传感器数据

---

## 八、核心技术亮点

### 后端

- **异步处理 + 进度追踪**: `@Async` 注解异步执行解析，通过 `UploadTask` 表 + 内存缓存双重支持进度查询
- **流式 CSV 解析**: OpenCSV 逐行读取，避免一次性加载大文件到内存导致 OOM
- **JPA 批量插入**: Hibernate `batch_size=1000` + `order_inserts`，`EntityManager` 每 1000 条 flush/clear 一次，显著提升大数量写入性能
- **多表头兼容**: 英文驼峰/下划线、中文别名均可识别，实际应用无需严格匹配表头

### 前端

- **双进度条**: 文件上传进度（axios onUploadProgress） + 后端解析进度（轮询 API），完整展示大文件从上传到入库的全过程
- **ECharts LTTB 降采样**: `sampling: 'lttb'` 算法在百万级数据点下保持折线形态，避免浏览器卡顿
- **综合视图多Y轴**: 最多 7 个 Y 轴左右交错展示，不同颜色区分
- **自动补全**: 航班号输入框自动提示已入库的航班号
- **响应式 + 窗口缩放适配**: 所有图表监听 `resize` 事件

---

## 九、性能与调优建议

1. **PostgreSQL**: 生产环境建议调大 `shared_buffers`、`work_mem`，为 `sensor_data.timestamp` 和 `flight_id` 创建组合索引
2. **JVM 参数**: 后端启动建议增加堆内存：
   ```bash
   java -Xmx4g -Xms2g -jar target/flight-sensor-backend-1.0.0.jar
   ```
3. **超大数据量**: 千万级以上记录建议引入时序数据库（TimescaleDB、InfluxDB）替换 PostgreSQL sensor_data 表
4. **上传优化**: 超大型 CSV 可在前端分片上传（chunk upload），后端合并后解析

---

## 常见问题

**Q: 上传大文件报错？**
   A: 后端已设置 `max-file-size: -1` 无限制。检查 Nginx 等反向代理的 `client_max_body_size`，前端代理已无限制。

**Q: CSV 解析后数据为 0 条？**
   A: 确认 timestamp 列存在且格式正确，查看上传任务列表中的 errorMessage。

**Q: 图表数据点太多浏览器卡死？**
   A: 查询时选择"300点"或"100点"采样，已内置 LTTB 算法。

**Q: 如何重置数据库？**
   A: 删除所有航班数据即可，或：
   ```sql
   DROP TABLE sensor_data, flights, upload_tasks CASCADE;
   ```
   后端重启会自动重建。
