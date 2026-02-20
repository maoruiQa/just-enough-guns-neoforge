# Mod Loading Fix - "Not a Valid Mod" 问题修复

**修复时间**: 2025-11-08
**问题**: Mod 显示 "not a valid mod file" 错误
**状态**: ✅ 已修复

---

## 问题原因

Mod 无法加载是因为缺少必需的 `META-INF/neoforge.mods.toml` 配置文件。

**错误日志**:
```
[ERROR] Skipping jar. File D:\...\build\classes\java\main is not a valid mod file
```

---

## 解决方案

### 1. 创建 neoforge.mods.toml 模板

将模板文件从 `src/main/templates/META-INF/neoforge.mods.toml` 复制到 `src/main/resources/META-INF/neoforge.mods.toml`

### 2. 配置 Gradle 处理模板变量

在 `build.gradle` 中添加 processResources 配置：

```groovy
// Process template files (neoforge.mods.toml)
tasks.named('processResources', ProcessResources).configure {
    def expandProps = [
        'mod_version': mod_version,
        'mod_id': mod_id,
        'mod_name': mod_name,
        'mod_license': mod_license,
        'mod_authors': mod_authors,
        'mod_description': mod_description,
        'neo_version': neo_version,
        'minecraft_version_range': minecraft_version_range
    ]

    inputs.properties(expandProps)

    // Only expand neoforge.mods.toml, not other resource files
    filesMatching('META-INF/neoforge.mods.toml') {
        expand(expandProps)
    }
}
```

### 3. 重新构建项目

```bash
./gradlew build
```

---

## 生成的配置文件

**位置**: `build/classes/java/main/META-INF/neoforge.mods.toml`

**内容**:
```toml
license="GNU General Public License v2.0 only"

[[mods]]
modId="jeg"
version="1.2.1"
displayName="Just Enough Guns"
authors="Rui Mao, MigaMi, Leander"
description='''Port of Just Enough Guns to NeoForge 1.21.1-1.21.4.'''

[[dependencies.jeg]]
    modId="neoforge"
    type="required"
    versionRange="[21.1.214,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.jeg]]
    modId="minecraft"
    type="required"
    versionRange="[1.21.1,1.21.5)"
    ordering="NONE"
    side="BOTH"
```

---

## 验证

构建成功后，日志中不再出现 "not a valid mod file" 错误：

```
[main/DEBUG] [ModFileParser/LOADING]: Considering mod file candidate ...build\classes\java\main
[main/DEBUG] [CommonLaunchHandler/CORE]: Found supplied mod coordinates [{jeg=[...\build\classes\java\main, ...\build\classes\java\client]}]
```

Mod 文件现在被正确识别为有效的 mod！

---

## 关键修改文件

1. ✅ `build.gradle` - 添加 processResources 配置
2. ✅ `src/main/resources/META-INF/neoforge.mods.toml` - 从模板复制
3. ✅ `gradle.properties` - 包含所有必需的变量

---

## 测试

运行客户端：
```bash
./gradlew runClient
```

Mod 现在可以正确加载并在游戏中可用！

---

**修复完成**: 2025-11-08
**状态**: ✅ 完全修复
