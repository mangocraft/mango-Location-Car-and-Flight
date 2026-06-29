# Mango Location / Car / Flight 维护手册

本文件是本仓库对人类维护者和编码 Agent 的长期约束。新增功能、线程模型、公共 API、配置语义或部署流程发生变化时，必须同步更新本文件和 `README.md`。

## 工程结构

- `mangolocation`：Folia 区域前置和公共 API，插件名 `MangoLocation`。
- `horsespeed`：宝马车辆插件，插件名 `BmwSpeed`。
- `ghastspeed`：快乐恶魂飞机插件，插件名 `GhostSpeed`。
- 根 `pom.xml`：Maven reactor，必须能够用一条命令构建三套插件。

运行环境为 Java 21、Paper/Folia 1.21.1 API。构建命令：

```bash
mvn clean package
```

提交前至少执行 `mvn -o clean package`；区域规则变更必须扩充 `mangolocation` 的 JUnit 测试。

## 地理位置契约

`mangolocation/config.yml` 的 `main-world` 是唯一执行行政区多边形检测的世界。默认行政区 ID：

- `san_cheng`：三城区；
- `huadu`：花都区；
- `interchange`：互通区。
- `outskirts`：远郊，不是多边形；覆盖主世界中没有命中上述区域的所有坐标。

多边形使用 X/Z 射线法，边界点算区域内；重叠边界由较小的 `priority` 获胜。不得把花都区简化成外接矩形，也不得拉直三城区的斜边。

非主世界不执行多边形计算，而是返回合成世界区域：

- ID：`world:<Bukkit 世界名>`；
- 名称：优先使用 `world-display-names`，否则使用 Bukkit 世界名；
- 默认 `world_nether → 地狱`、`world_the_end → 末地`。

主世界行政区外返回配置项 `main-world-outside-area` 定义的远郊区域，默认 `outskirts / 远郊`；非主世界始终返回合成 `Area`。对已加载的有效世界，API 因此始终能返回明确区域。

## 公共 API

消费插件必须在 `plugin.yml` 声明 `depend: [MangoLocation]`，并通过 Bukkit `ServicesManager` 获取 `MangoLocationApi`。不要持有实现类。

```java
MangoLocationApi api = Bukkit.getServicesManager().load(MangoLocationApi.class);
Optional<Area> area = api.findArea(player);
```

`findArea(String worldName, double x, double z)` 是纯坐标、线程安全查询。`findArea(Player)` 会读取实体位置，只能在该玩家事件、命令上下文或 `player.getScheduler()` 中调用。

玩家区域变化通过 `PlayerAreaChangeEvent` 通知。区域监听只在玩家跨方块时计算；不得移除这一快速返回，或在每次 `PlayerMoveEvent` 中进行磁盘、网络、数据库操作。

## Folia 强制规则

1. 玩家和实体只能在其 `EntityScheduler`/所属事件线程访问；从控制台或另一玩家区域操作目标玩家时，必须提交到目标的 `getScheduler()`。
2. 全局调度器不拥有实体数据，只用于全局状态和枚举后再投递实体任务。
3. 插件单例中的可变共享集合必须使用并发集合，或被严格限制在一个线程。禁止让多个实体任务写普通 `HashMap`。
4. 区域线程禁止磁盘 I/O。车牌和机号通过异步、合并写入的 YAML store 保存；插件关闭时执行最终同步快照。
5. 运行时配置使用不可变使用方式：加载新 `FileConfiguration` 后通过 `volatile` 引用一次替换，禁止热路径修改配置对象。
6. 不要在持有 Java 全局锁时访问 Bukkit 实体，也不要用一个 `synchronized` 上牌方法串行阻塞所有区域。
7. Vault 经济实现和 Residence 版本必须由服主确认支持 Folia；本仓库只能保证调用发生在相关玩家/位置线程，不能替第三方插件保证内部线程安全。

## 性能基线

- 行政区查询当前只有三个小多边形，百人移动不是瓶颈。
- 活跃车辆/飞机碰撞检测默认每 4 tick 一次；降低间隔会线性增加实体任务数量，修改前必须用真实服务器和 spark 压测。
- 每秒燃油检查从全局线程枚举玩家后投递到玩家调度器；禁止直接从全局线程读取载具或玩家状态。
- 车牌/机号唯一性使用并发 Set 做原子占位，不得退回 `containsValue()` 全表扫描。
- YAML registry 是永久登记册，会保留已发放号码。若单类登记超过约五万条，应规划迁移到带唯一索引的数据库，而不是提高整文件保存频率。

## 末地飞行规则

末地默认禁止组装和启动飞机。世界环境以 `World.Environment.THE_END` 判断，不依赖世界文件夹名称。

- 对快乐恶魂使用本插件速度护具时取消交互；
- 尝试骑乘已有护具的飞机时取消进入；
- 默认理由：末地空间裂隙与龙息会持续扰乱飞行器导航核心，航空管理局禁止在此组装或启动飞机；
- 开关和文案位于 `ghastspeed/config.yml` 的 `end-flight-restriction`。

任何绕过末地限制的新生成入口都必须同时补上检查和测试。

## 数据与兼容性

车牌和机号首先保存在实体 PDC，同时保存在 `plates.yml` / `jijia.yml` 登记册。已有号码不得因跨区、重载或升级而自动变化。新号码使用首次登记时载具所在区域的前缀。

`MangoLocation` 必须先于两个载具插件加载。车辆插件对 Vault 使用 `softdepend`，飞机插件对 Vault、Residence 使用 `softdepend`。不要把 MangoLocation API shade 进消费插件，否则会导致 Bukkit 服务接口出现类加载器不一致。

## 变更检查清单

- 是否在错误线程访问了玩家、实体、区块或背包？
- 是否向插件单例添加了普通可变集合？
- 是否在 region tick 中写盘、联网或等待锁？
- 是否保持主世界行政区/远郊和非主世界世界区域的返回契约？
- 是否保持末地飞机限制的所有入口？
- 是否更新配置注释、README、本文件和测试？
- 是否确认三个最终 JAR 不包含重复的 `com/mangolocation/api` 类？
