# 1.21.1 NeoForge 枪械位移规则说明（当前分支）

本文档说明当前分支里，一人称持枪/瞄准（ADS）位移是如何计算和应用的。

核心实现文件：
- `src/main/java/ttv/migami/jeg/client/GunItemClientExtensions.java`
- `src/main/java/ttv/migami/jeg/client/handler/AimingHandler.java`

## 1. ADS 进度来源

`AimingHandler` 提供 `ads`（范围 `0.0 ~ 1.0`）：
- `0.0`：完全 hip-fire（未瞄准）
- `1.0`：完全 ADS（已瞄准）

进度计算逻辑：
- 每 tick 按固定速度推进/回退（`AIM_SPEED = 1.0F`，最大 `MAX_AIM_PROGRESS = 5.0F`）
- 渲染时按 `partialTick` 插值，得到平滑 ADS 进度

## 2. 每把枪的“基础姿态参数”

在 `GunItemClientExtensions` 中先按枪型选择一组参数：
- hip 参数：`hipX/hipY/hipZ`、`hipYaw`
- ADS 参数：`adsX/adsY/adsZ`、`adsYaw`
- 缩放：`scale`

当前分支的分类：
- `finger_gun`
- `rocket_launcher`
- `bow`
- `typhoonee`
- `double_barrel`
- `shortWeapon`（pistol/revolver/grenade_launcher/flare_gun/double_barrel/waterpipe）
- 默认长枪分支

## 3. 基础插值

先做线性插值：

- `x = lerp(ads, hipX, adsX)`
- `y = lerp(ads, hipY, adsY)`
- `z = lerp(ads, hipZ, adsZ)`
- `yaw = lerp(ads, hipYaw, adsYaw)`

这一步决定“从 hip 到 ADS”的主过渡轨迹。

## 4. 分支叠加修正（在插值后）

当前分支还会叠加一层“规则修正”：

### X（左右）
- 全局 ADS 左移：`x -= ads * 0.02`
- 火箭筒 ADS 极轻微右移：`x += ads * 0.03`
- 手枪类（`pistol/revolver`）ADS 右修正：`x += ads * 0.04`
- `typhoonee` 和 `flare_gun` ADS 右修正：`x += ads * 0.04`

### Y（上下）
- 所有枪 ADS 小幅上移：`y += ads * 0.05`
- 非火箭筒额外中小幅上移：`y += ads * 0.07`

### Yaw（朝向）
- 非 `finger_gun` 的全局 ADS 左偏：`yaw += 1.2 * ads`
- 手枪类额外修正：`yaw += lerp(ads, -0.3, 1.0)`
- `typhoonee`/`flare_gun` ADS 额外左偏：`yaw += 0.6 * ads`

## 5. 变换应用顺序

`PoseStack` 顺序如下：
1. `translate(direction * x, y, z)`
2. `rotate Y by direction * yaw`
3. `rotate Z by 0`（当前分支关闭侧倾）
4. `rotate X by 4°`
5. `scale(scale, scale, scale)`
6. 应用装备动画下压：`translate(0, -0.6 * equip, 0)`
7. 非弓类再叠加轻微挥动平移

注：`direction` 为左右手方向，右手 `+1`，左手 `-1`。

## 6. 如何调参（建议）

若要继续微调，优先按这三层修改：
1. 分类基础参数（`hip* / ads*`）：决定整体手感
2. 插值后修正（`x/y/yaw` 的 `ads * k`）：做小步纠偏
3. 统一视觉参数（`scale`、`X 轴固定角度`）：统一“大小/抬头感”

常见症状与对应参数：
- ADS 太低：增大 `y` 的 ADS 叠加系数
- ADS 偏左：减小全局左移或增加对应分支右修正
- 枪口朝右：增大 `yaw` 的左偏系数（正值）
- 过于“歪斜”：保持 Z 轴旋转为 `0`

## 7. 关联文件

本分支下与该逻辑直接相关的文件：
- `src/main/java/ttv/migami/jeg/client/GunItemClientExtensions.java`
- `src/main/java/ttv/migami/jeg/client/handler/AimingHandler.java`
