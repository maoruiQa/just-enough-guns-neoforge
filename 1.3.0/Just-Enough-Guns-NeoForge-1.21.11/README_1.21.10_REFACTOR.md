# Just Enough Guns - NeoForge 1.21.10

重构完成时间：2025年10月29日

## 重构过程

1. **创建新项目目录**: 基于justenoughgunsforneoforge12110-template-1.21.10模板创建新的1.21.10项目
2. **复制源码**: 从Just-Enough-Guns-NeoForge-1.21.9版本复制所有Java源代码和资源文件
3. **更新配置**:
   - 更新gradle.properties以匹配项目配置（mod_id=jeg, mod_group_id=ttv.migami.jeg等）
   - 添加flatDir repository以支持本地库文件
4. **复制依赖库**:
   - GeckoLib: geckolib-neoforge-1.21.8-5.2.2.jar
   - Framework: framework-neoforge-1.21.8-0.12.3.jar
5. **构建验证**: 成功构建生成jeg-1.1.4.jar

## 版本信息

- **Minecraft版本**: 1.21.10
- **NeoForge版本**: 21.10.16-beta
- **Java版本**: 21
- **Mod版本**: 1.1.4
- **Mod ID**: jeg

## 项目结构

```
Just-Enough-Guns-NeoForge-1.21.10/
├── src/main/java/ttv/migami/jeg/    # Java源代码
├── src/main/resources/              # 资源文件（assets, data）
├── src/main/templates/              # 模板文件（neoforge.mods.toml）
├── libs/                            # 本地依赖库
│   ├── geckolib-neoforge-1.21.8-5.2.2.jar
│   └── framework-neoforge-1.21.8-0.12.3.jar
├── build.gradle                     # 构建配置
├── gradle.properties               # 项目属性
└── build/libs/jeg-1.1.4.jar        # 生成的mod文件
```

## 构建状态

✅ **构建成功** - 所有源码编译完成，无错误
✅ **依赖库就绪** - GeckoLib和Framework已正确配置
✅ **配置文件更新** - 所有配置已适配1.21.10版本

## 使用说明

1. 进入项目目录：`cd Just-Enough-Guns-NeoForge-1.21.10`
2. 构建项目：`./gradlew build`
3. 运行客户端：`./gradlew runClient`
4. 运行服务器：`./gradlew runServer`

生成的jar文件位于：`build/libs/jeg-1.1.4.jar`