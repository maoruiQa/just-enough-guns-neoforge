# Just Enough Guns New — 玩家与服务器 Wiki

**规范语言：** English · **当前版本：** `1.8.2`

[Wiki 索引](README.md) · [English](en-US.md) · [日本語](ja-JP.md) · [Deutsch](de-DE.md) · [Español](es-ES.md)

## 关于本模组

Just Enough Guns New 是 MigaMi Forge 1.20.1 **Just Enough Guns** 的非官方现代移植版。在保留原版风格生存进程的同时，加入枪械、弹匣、配件、敌对枪手、派系袭击、Walkürenritt 载具、特殊装备和空中威胁。

项目按 Fabric 与 NeoForge 分成独立模块。服务器和所有加入的客户端必须使用相同的 Minecraft 版本、加载器系列和模组版本。

## 快速开始

1. 在[兼容性表](#兼容性)中选择与你的 Minecraft 版本和加载器匹配的行。
2. 安装对应加载器、Fabric API 或 NeoForge，以及表中列出的 GeckoLib 版本。
3. 将匹配的 `jegn-1.8.2` JAR 放入实例的 `mods` 文件夹。不要混用 Fabric 和 NeoForge JAR。
4. 启动一次游戏，创建或复制测试世界，并确认模组列表中出现 Just Enough Guns。
5. 在确认配方、按键、服务器配置和附加模组依赖之前，不要直接在长期运行的服务器上测试。

### 第一次游戏检查清单

- 制作或找到合适的工作台、弹药和兼容弹匣。
- 将正确的散装弹药装入弹匣，再把弹匣装入使用弹匣的武器。
- 在按键设置中检查冲突，然后测试射击、瞄准、换弹和检视。
- 携带备用弹匣、维修或冷却物品，并为武器的射击模式准备足够弹药。
- 新服务器先从普通枪手遭遇开始，再考虑开启大型袭击、载具或 Terror Phantom 事件。

### 使用游戏内战斗生涯指南

1.8.2 增加了原生进度指南，分为 **Ready for Action**、**Front-line Experience**、**Special Operations** 和 **Steel and Expeditions** 四章。打开进度界面，跟随第一个可见目标，就能知道下一步需要哪个工作台、武器或载具。指南覆盖代表性装备，许多目标允许同类替代装备完成。

只有服务端确认成功的行为才会计入进度。失败射击、友方目标、创造/旁观模式行为，以及单纯拥有物品，都不会完成玩法目标。载具目标需要实际移动；直升机目标需要受控上升和着陆。它是学习路线，不要求收集每一种武器。

## 兼容性

| 加载器 | Minecraft | Java | 模组 | 必需依赖 |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.2 | Fabric API、GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.2 | NeoForge 21.1.x、GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.2 | Fabric API、GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.2 | NeoForge 26.2.x、GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.2 | Fabric API、GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.2 | NeoForge 26.3.x、GeckoLib 5.5.7 |

Fabric 26.1 和 NeoForge 26.1 属于旧版维护线。除非整合包明确要求 26.1，否则 Java 25 维护线应使用 26.2。

## 控制

### 枪械

| 动作 | 默认按键 |
| --- | --- |
| 射击 | 鼠标左键 |
| 瞄准 | 鼠标右键 |
| 换弹 | `R` |
| 检视动画枪械 | `Y` |
| 枪托近战或手电切换（支持时） | `V` |
| 潜行相关行为（支持时） | `Shift` |

这些是 1.3.0 及更高版本的默认设置。更早版本使用右键射击、`F` 换弹和 `Shift` 瞄准。升级后请重新检查按键设置。

### 载具

| 动作 | 默认按键 |
| --- | --- |
| 登上或交互 | 鼠标右键 |
| 转向与加速 | `W` / `A` / `S` / `D` |
| 刹车或倒车 | `S` |
| 查看或瞄准车载武器 | 鼠标移动 |
| 发射当前武器 | 鼠标左键 |
| 瞄准、锁定、缩放或使用副功能 | 鼠标右键 |
| 换弹（支持时） | `R` |
| 下车 | `Shift` |

座位决定可用动作。驾驶位负责移动，武器位或副驾驶位可能负责炮塔、导弹、目标锁定或干扰设备。

## 游戏玩法

### 枪械与弹药

武器包括手枪、左轮手枪、步枪、冲锋枪、霰弹枪、机枪、发射器、弓、喷火器风格武器和后期特殊枪械。大多数武器需要匹配的弹药或弹匣。

使用弹匣的武器必须先装填弹匣；手动装填或单发武器直接使用对应弹药。部分步枪、冲锋枪和霰弹枪支持加长弹匣与鼓式弹匣。配件、枪托、握把、瞄具、皮肤、徽章和特殊弹药属于正常生存进程。

首次加入服务器时可能出现 **服务器弹药规则** 提示框。**弹匣供弹**要求把弹药装进实体弹匣，再通过更换弹匣给支持的枪械换弹；加长弹匣和鼓式弹匣配方可用。**直接供弹**则从背包中的散装弹药换弹，并恢复旧式容量配件。点击 **知道了** 或按 `Esc`，确认状态按玩家保存。服务器切换模式时会再次提示并刷新配方，不会删除或自动转换已有物品。

后坐力、移动散布、过热、动态准星、命中反馈、枪口效果和子弹轨迹会显示武器状态。武器无法射击时，先检查弹药、弹匣、热量、换弹状态和服务器配置。

### 防弹保护

防弹头盔和背心通过穿甲值与护甲 rating 的比较来拦截枪械伤害，而不是使用原版 Projectile Protection。有效穿甲值来自弹药和枪械倍率。头部命中优先使用头盔，其他身体命中使用背心；没有对应装备时，枪械伤害不变。

低于护甲值的攻击仍会造成部分伤害并消耗耐久；超过护甲值的攻击伤害更高，也会施加更大的耐久压力。具体数值属于各分支数据，可能随平衡更新变化；特定版本应以游戏内提示和对应分支源码为准。

### 枪手、派系与袭击

枪手可以出现在僵尸、骷髅、猪灵、掠夺者/卫道士、幻翼、尸鬼和干枯等家族。派系事件会从巡逻和派系不祥之兆发展为回家触发的袭击、袭击信号、Boss 条和可配置波次。C4 背心轰炸枪手等变体由服务器配置控制。

### 载具

Walkürenritt 内容包括组装式陆地载具、船、飞机、直升机和固定武器平台。载具拥有座位、库存、维修工具、充能或能源支持、导弹、诱饵以及专用 HUD。

先完成载具组装进程，再在世界中部署。将载具需要的弹药、维修工具和充能物品放入正确库存，并观察 HUD 上的武器准备、换弹、导弹锁定、损伤和干扰设备状态。

敌方载具 AI 可以巡逻、追击、倒车、避开不适合的地形，并按车型控制炮塔。维护分支共享玩家可见的载具行为，但加载器实现仍然独立。

### 特殊装备

- **FPV 无人机：** 使用显示器控制视角和载荷；爆炸载荷可以进入引导式自杀俯冲。
- **C4 与阔剑地雷：** 放置后使用正确的雷管或触发器；拆除敌方 C4 时携带 C4 拆除器。
- **C4 背心：** 可配置的轰炸枪手变体会携带爆炸背心。
- **Javelin 与 Igla 9K38：** 目标处于射程且有视线时使用锁定；烟幕可以阻断导弹锁定。
- **载具导弹锁定 HUD：** 合法且可见的目标进入范围后会显示搜索框和音效反馈。

### Terror Phantom

Terror Phantom 是稀有空中威胁，包含 Bound Terror Phantom、幻翼枪手召唤物、可配置死亡爆炸和 End Ship Armada 事件。1.8.0 特殊装备线默认软禁用自然 Terror Phantom 生成；服务器管理员可以在配置中重新启用或调整。

## 太难还是太简单？

一次只调整一组设置，先在游戏中观察几天，并在改动进程前复制世界。下面的命令以 Fabric 26.2 分支为准。

| 玩家感受 | 第一个调整 | 作用 |
| --- | --- | --- |
| 开局枪手太强 | `/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled false` | 不再让自然枪手匹配附近最强生存玩家的装备，同时保留按时间成长。 |
| 每次回家都遇到巡逻 | `/justEnoughGuns config patrol enabled false` 或 `/justEnoughGuns config patrol minimumDays 15` | 关闭自然巡逻，或推迟最早生成天数。 |
| 巡逻本身可以接受，但出现太频繁 | `/justEnoughGuns config patrol intervalDays 10` 和 `/justEnoughGuns config patrol spawnChance 0.15` | 延长固定间隔，并降低最终生成判定。 |
| 回家袭击太难 | 在服务端配置界面或 `config/jeg-server.toml` 中关闭 `factionRaid.dynamicDifficultyEnabled` | 不再按附近装备、世界时间和队伍人数动态提高袭击难度。 |
| 腰射几乎打不中 | `/justEnoughGuns config combat hipFireSpreadMultiplier 1.0` | 默认值是 `1.5`；降低后腰射更集中。瞄准仍然保留应有的精度优势。 |
| 弹匣管理太麻烦 | `/justEnoughGuns config combat magazineFeed false` | 支持的枪械改为直接消耗散装弹药换弹，并刷新弹药规则提示和配方。 |
| 想让弹匣系统更重要 | `/justEnoughGuns config combat magazineFeed true` | 要求使用兼容实体弹匣，并启用支持武器的加长/鼓式弹匣配方。 |
| 火箭筒或 C4 出现太早 | 服务端 UI 选择 **生物**，切换到 **全部枪手**，提高 `火箭筒起始天数` 或 `爆破枪手起始天数` | 推迟特殊威胁，不会删除普通枪手。 |
| 敌方载具压垮新基地 | `/justEnoughGuns config vehicle enemySpawning enabled false` | 关闭自然敌方载具转化和袭击载具增援，但保留玩家载具。 |
| 想要更早进入载具后期 | 编辑 `config/jeg-server.toml` 中的 `vehicle.enemyVehicleStartDay`、`vehicle.enemyVehicleConversionChance` 和 `vehicle.enemyVehicleMaxConversionChance` | 调整敌方载具压力，不关闭整个载具系统。 |
| 爆炸镜头晃动太强 | 在 `config/jeg-client.toml` 中设置 `rendering.explosionScreenShake = 0` | 只关闭本地镜头反馈，不改变伤害。 |

### 不要先这样调

如果只是袭击太难，不要把所有枪手概率都设为零；巡逻、派系袭击、自然枪手成长和敌方载具是分开的系统。成长设置中的 `-1` 表示继承平衡默认值或 **全部枪手** 覆盖值，不要不理解继承关系就把所有值改成 `0`。

## 游戏内服务端配置界面

Fabric 26.2 客户端会在暂停菜单加入 **JEGN 配置** 按钮。服务器必须运行同一 JEG 版本，玩家需要 OP 权限等级 2。单人世界需要允许作弊/命令；专用服务器可使用 `/op <玩家>` 或服务器权限管理器。

1. 按 `Esc` 打开暂停菜单。
2. 点击菜单上方的 **JEGN 配置**。
3. 选择 **界面**、**巡逻**、**生物**、**战斗** 或 **载具**。
4. 布尔项点击切换，数值项直接输入；在 **生物** 分类使用左右按钮选择枪手类型，再编辑成长项。
5. 将鼠标悬停在标签上，可看到命令键、允许范围和 `-1` 继承规则。
6. 点击 **应用**。服务端会校验值、保存 `config/jeg-server.toml`，必要时刷新弹匣配方并广播 UI 设置。
7. **恢复默认值**只恢复当前分类；点击 **完成**返回暂停菜单，如果有未保存改动会先询问是否放弃。

界面可实时修改的项目包括：

- **界面：** `ui.showCrosshair`、`ui.showHitFeedback`，以及存在于当前分支的 `ui.hideMedals`。
- **巡逻：** `patrol.enabled`、`patrol.intervalDays`（`0–30`）、`patrol.minimumDays`（`0–100`）、`patrol.spawnChance`（`0.0–1.0`）。
- **生物：** 幻翼枪手死亡爆炸、自然枪手动态难度，以及 `all`、`skeleton`、`stray`、`zombie`、`husk`、`parched`、`drowned`、`zombieVillager`、`zombifiedPiglin`、`piglin`、`piglinBrute`、`witherSkeleton`、`pillager`、`vindicator`、`generic` 共 15 个枪手成长档案。
- **战斗：** 子弹破坏方块、弹匣供弹、爆头倍率、腰射散布（`0.0–5.0`）、枪手地形支撑、最大地形破坏等级（`0–3`）和派系袭击动态难度。
- **载具：** 载具系统、敌方自适应战斗、敌方载具生成、起始天数（`0–5000`）和转化概率（`0.0–1.0`）。

如果按钮不存在或点击后返回暂停菜单，先检查权限和服务端日志。配置请求由服务端权威处理，客户端编辑自己的客户端文件不能修改服务器。

## 命令参考

所有命令都以 `/justEnoughGuns` 开头。每输入一个词后按 `Tab` 查看当前分支建议。不写最后的值时会读取当前值；写入值则会修改它。设置值需要权限等级 2。

### 玩家与测试世界命令

```text
/justEnoughGuns unlockGunRecipes
/justEnoughGuns spawnPatrol <faction> <size> <pos> [forceGuns] [spawnRadius]
/justEnoughGuns simulatePatrol <faction> <size> <player> [forceGuns]
```

`unlockGunRecipes` 只要求玩家实体，会直接解锁全部 JEG 枪械配方，适合测试世界或管理员决定，不建议用于正常生存进程。内置派系名为 `night_of_the_undead`、`the_rattlers`、`nosy_business`、`bad_piggies`、`hell_hogs`、`lost_souls`。巡逻规模为 `1–20`，生成半径为 `0–16`，省略时默认为 `10`。

```text
/justEnoughGuns spawnPatrol night_of_the_undead 6 ~ ~ ~ true 10
/justEnoughGuns simulatePatrol the_rattlers 4 @s true
```

和平难度下不能生成枪手巡逻；巡逻命令需要管理员权限。`simulatePatrol` 适合不等待自然事件就检查某个派系的武器配置。

### 配置命令树

```text
/justEnoughGuns config ui crosshair [true|false]
/justEnoughGuns config ui hitFeedback [true|false]

/justEnoughGuns config patrol enabled [true|false]
/justEnoughGuns config patrol intervalDays [0-30]
/justEnoughGuns config patrol minimumDays [0-100]
/justEnoughGuns config patrol spawnChance [0.0-1.0]

/justEnoughGuns config mob mechanism terror chance [0.0-1.0]
/justEnoughGuns config mob mechanism terror max [0.0-1.0]
/justEnoughGuns config mob mechanism phantom deathExplosion [true|false]
/justEnoughGuns config mob spawn <type> <setting> [value]

/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled [true|false]
/justEnoughGuns config combat blockDamage [true|false]
/justEnoughGuns config combat magazineFeed [true|false]
/justEnoughGuns config combat headshotMultiplier [true|false]
/justEnoughGuns config combat hipFireSpreadMultiplier [0.0-5.0]
/justEnoughGuns config combat gunnerTerrainPlacement enabled [true|false]
/justEnoughGuns config combat gunnerTerrainPlacement block <block_id>
/justEnoughGuns config combat gunnerTerrainBreak maxTier [0-3]

/justEnoughGuns config factionRaid dynamicDifficultyEnabled [true|false]

/justEnoughGuns config vehicle enabled [true|false]
/justEnoughGuns config vehicle enemyVehicleAdaptiveCombatEnabled [true|false]
/justEnoughGuns config vehicle enemySpawning enabled [true|false]
/justEnoughGuns config vehicle enemySpawning startDay [0-5000]
/justEnoughGuns config vehicle enemySpawning conversionChance [0.0-1.0]
/justEnoughGuns config vehicle enemySpawning maxConversionChance [0.0-1.0]
/justEnoughGuns config vehicle enemySpawning conversionChancePerDay [0.0-1.0]
```

例如，下面这组命令会降低开局压力，但不删除武器：

```text
/justEnoughGuns config patrol minimumDays 15
/justEnoughGuns config patrol spawnChance 0.15
/justEnoughGuns config combat hipFireSpreadMultiplier 1.0
/justEnoughGuns config vehicle enemySpawning startDay 120
```

枪手成长命令的 `type` 可使用上述 15 个类型。`setting` 包括：

| 设置 | 含义 |
| --- | --- |
| `minSpawnChance`、`maxSpawnChance`、`spawnChancePerDay` | 枪手转化概率下限、上限和每日增量。 |
| `weaponInitialTier`、`weaponMaxTier`、`weaponTierPerDay` | 初始武器等级、最高等级和每日增量。 |
| `armorInitialTier`、`armorMaxTier`、`armorTierPerDay` | 初始护甲等级、最高等级和每日增量。 |
| `rocketLauncherStartDay`、`rocketLauncherChance`、`rocketLauncherMaxChance`、`rocketLauncherChancePerDay` | 火箭筒出现天数和概率。 |
| `bomberStartDay`、`bomberChance`、`bomberMaxChance`、`bomberChancePerDay` | C4 爆破枪手出现天数和概率。 |
| `weaponAggression` | 该档案选择更激进武器角色的倾向。 |

例如，把所有枪手的爆破枪手推迟到第 100 天：

```text
/justEnoughGuns config mob spawn all bomberStartDay 100
/justEnoughGuns config mob spawn all bomberChance 0.03
/justEnoughGuns config mob spawn all bomberMaxChance 0.06
```

## 配置文件

实时修改优先使用命令或服务端界面。直接编辑前先停止对应实例，否则运行中的服务端可能覆盖文件。

### 客户端文件：`config/jeg-client.toml`

这些设置只影响本地玩家的显示：

| 键 | 默认值 | 玩家效果 |
| --- | --- | --- |
| `rendering.showAmmoHud` | `true` | 显示武器弹药面板。 |
| `rendering.showTimersHud` | `true` | 显示过热、冷却等计时条。 |
| `rendering.crosshair` | `"jeg:dynamic"` | 可用 `default`、`jeg:dynamic`、`jeg:tech` 或自定义准星纹理 ID。 |
| `rendering.showHitmarker` | `true` | 生物命中时显示短暂标记。 |
| `rendering.dynamicCrosshairDotMode` | `"at_min_spread"` | 可用 `never`、`at_min_spread`、`threshold`、`always`。 |
| `rendering.dynamicCrosshairDotThreshold` | `0.8` | `threshold` 模式的散布阈值。 |
| `rendering.explosionScreenShake` | `100` | `0–100` 的镜头震动强度；`0` 为关闭。 |
| `rendering.legacyBulletTrailEnabled` | `true` | 是否使用旧版子弹轨迹渲染。 |
| `rendering.hideAttachmentConfigButton` | `false` | 是否隐藏配件界面的配置按钮。 |
| `rendering.attachmentButtonAlignment` | `"right"` | 可用 `left` 或 `right`。 |

### 服务端文件：`config/jeg-server.toml`

主要分组为 `ui`、`attachments`、`spawns`、`gunnerGrowth`、`combat`、`terrorRaid`、`factionPatrol`、`factionRaid` 和 `vehicle`。玩家最常调整的键包括：

- `combat.magazineFeed`：选择实体弹匣或散装弹药换弹。
- `combat.hipFireSpreadMultiplier`：默认 `1.5`，范围 `0.0–5.0`。
- `combat.naturalGunnerDynamicDifficultyEnabled`：是否让自然枪手适应附近生存玩家装备。
- `combat.gunnerTerrainPlacementEnabled`、`combat.gunnerTerrainSupportBlock`、`combat.gunnerTerrainBreakMaxTier`：枪手是否放置支撑方块，以及允许破坏的地形等级。
- `factionPatrol.enabled`、`factionPatrol.intervalDays`、`factionPatrol.minimumDays`、`factionPatrol.spawnChance`：巡逻频率和最早天数。
- `factionRaid.enabled`、`factionRaid.minimumDays`、`factionRaid.homeTriggerRadius`、`factionRaid.dynamicDifficultyEnabled`：袭击开关、触发条件和难度缩放。
- `vehicle.enemyVehicleSpawningEnabled`、`vehicle.enemyVehicleStartDay`、`vehicle.enemyVehicleConversionChance`、`vehicle.enemyVehicleMaxConversionChance`：敌方载具压力。

`combat.magazineFeed` 改变后，在线玩家会立即收到弹药规则提示；启动、`/reload` 和配置修改也会刷新受模式限制的配方。已有物品不会被删除或自动转换。

## 服务器管理

优先使用上面的[游戏内服务端配置界面](#游戏内服务端配置界面)做实时调整，用[命令参考](#命令参考)做快速试验，用 `config/jeg-server.toml` 管理未暴露在界面的高级选项。直接编辑 TOML 前先停止服务器并备份世界；游戏内 **Apply** 会验证、保存并广播需要同步的设置。

## 故障排查

### 游戏无法加载

按照兼容性表确认 Minecraft、加载器、Java 和 GeckoLib 版本。删除重复或跨加载器 JAR，再只保留必要依赖和 Just Enough Guns 进行测试。

### 专用服务器启动时崩溃

确认 JAR 与服务器加载器和 Minecraft 版本匹配。不要在 NeoForge 服务器放入客户端专用附加模组或 Fabric JAR，反之亦然。使用干净测试实例复现，并保留第一次失败启动的完整日志。

### 缺少枪械、配方或载具

重启后检查配方书、物品搜索和服务器日志。确认服务器与客户端使用同一个模组文件，依赖也来自同一 Minecraft 版本。报告缺失配方时，请提供物品 ID 和日志行。

### 按键或 HUD 不显示

在按键界面搜索冲突，恢复默认绑定，并在启用动态准星、命中标记和弹药 HUD 的情况下测试。载具 HUD 还取决于座位和当前武器。

### 导弹无法锁定

检查距离、视线、目标类型、弹药和发射器状态。烟幕会主动阻断导弹锁定。只有在射程内且可见的合格目标才应显示锁定框。

## Bug 报告

请在仓库 issue 中提供：

1. Minecraft 版本和加载器（Fabric 或 NeoForge）。
2. Just Enough Guns 版本和精确 JAR 文件名。
3. Java 版本以及 Fabric API、NeoForge、GeckoLib 版本。
4. 简短复现步骤，包括世界状态和相关配置。
5. 完整崩溃报告或最新日志；视觉或音频问题请附截图或录屏。
6. 在没有其他附加模组的干净实例中是否仍能复现。

不要只写“会崩溃”或只发启动器截图。准确的版本矩阵和第一段有效堆栈通常能判断问题属于加载器、依赖、本模组还是其他附加模组。

## 开发者与维护者链接

- [根 README](../../README.md) 与 [description 副本](../../description.md)
- [1.8.2 发布说明](../../CHANGELOG.md)
- [1.8.0 功能说明](../../CHANGELOG.md)
- [成就指南](../../docs/ADVANCEMENT_GUIDE.md)
- [敌方载具 AI 说明](../../docs/vehicle_enemy_ai.md)
- [验证说明](../../docs/VALIDATION.md)

修改玩家可见行为时，先更新英文页面，再同步四份翻译。分支实现细节留在对应模块的 `docs/` 目录。

## 发布、致谢与许可证

公开的 1.8.2 版本增加四章进度指南、服务器弹药规则提示、实时配置反馈和载具操作调整；1.8.1 修复了维护分支的专用服务器启动路径和服务器配置选项处理。1.8.0 功能版本加入 FPV 无人机、C4、阔剑地雷、C4 背心、Javelin、Igla、烟幕拒锁、导弹锁定 HUD、击杀归属修复以及载具/导弹/火箭平衡调整。

Just Enough Guns New 是非官方移植项目，与 Just Enough Guns 或 Superb Warfare 没有隶属或背书关系。基于 Just Enough Guns 的代码使用 GPL-3.0。原 JEG 素材为 ARR，并在作者授权下使用。SBW 衍生载具和特殊装备素材保留其署名与许可证要求；完整致谢请查看根 README。
