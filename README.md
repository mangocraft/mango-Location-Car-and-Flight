# MangoLocation 区域前置

本仓库包含三个 Folia 插件：

- `mangolocation`：区域判断与跨区事件 API；
- `horsespeed`：宝马车辆插件，新车会按登记地点生成区域车牌；
- `ghastspeed`：飞机插件，新飞机会按登记地点生成区域机号。

## 构建与安装

在仓库根目录执行：

```bash
mvn clean package
```

将以下文件放入服务器 `plugins/`，并确保 MangoLocation 一同安装：

- `mangolocation/target/mangolocation-1.1.0.jar`
- `horsespeed/target/horsespeed-1.2.jar`
- `ghastspeed/target/ghastspeed-1.2.jar`

区域在 MangoLocation 的 `config.yml` 中配置。只有 `main-world` 执行行政区多边形检测；其他世界返回 `world-display-names` 中的显示名，例如“地狱”和“末地”。重载命令为 `/mangolocation reload`，玩家可直接用 `/area` 查询当前位置。

## 供其他插件调用

把 `com.mangolocation:mangolocation:1.1.0` 作为 `provided` 依赖，并在 `plugin.yml` 添加：

```yaml
depend: [MangoLocation]
```

获取 API：

```java
MangoLocationApi api = Bukkit.getServicesManager().load(MangoLocationApi.class);

if (api == null) {
    getLogger().severe("未找到 MangoLocation API");
    return;
}

String areaId = api.findArea(player)
        .map(Area::id)
        .orElse("outside");

String areaName = api.findArea(player)
        .map(Area::name)
        .orElse("未划定区域");
```

如果手里只有坐标，也可以调用 `api.findArea(worldName, x, z)`。主世界行政区外返回空值；其他世界总会返回世界区域，ID 格式为 `world:<世界名>`。

监听跨区提醒或执行其他逻辑：

```java
@EventHandler
public void onAreaChange(PlayerAreaChangeEvent event) {
    event.getToArea().ifPresent(area ->
            event.getPlayer().sendMessage("进入了 " + area.name()));
}
```

边界重叠时使用 `priority` 数字更小的区域。点落在边线上也算区域内。车牌/机号前缀分别在两个载具插件的 `registration.area-prefixes` 下配置；现有已登记载具保持原号码。末地默认禁止安装飞机护具或启动飞机，理由和开关位于飞机插件的 `end-flight-restriction`。
