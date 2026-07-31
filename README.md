# MemorySweep（Babric / StationAPI 版）

一个适用于 **Minecraft Beta 1.7.3(Babric + StationAPI 前置)** 的自动内存清理模组。

## ⚠️ 先看这里:为什么手动触发是"快捷键"而不是"/指令"

Beta 1.7.3(2010 年底)比现代 Minecraft 的 `/command` 系统(Brigadier)早了好几年。经过实际查证:

- 原版 Beta 1.7.3 本身没有可扩展的聊天指令系统。
- StationAPI(本模组依赖的前置)也没有提供指令注册或聊天消息拦截相关的模块。
- 有个叫 "Legacy Brigadier" 的模组能加指令系统,但它是给另一套不兼容的生态("Cursed Legacy API")做的,和 StationAPI 混用大概率冲突,不适合作为前置一起用。
- 如果硬要用 Mixin 直接拦截聊天包实现指令,现有的社区映射表里这部分方法还没有可读名字(仍是混淆状态),没法在不反编译真实游戏 jar 的情况下写出可靠的 Mixin 目标——写错 Mixin 目标是直接崩溃开局,风险较高。

所以这一版用**客户端快捷键**(默认按 **J**,可在游戏内"控制选项"里重新绑定)替代 `/memorysweep` 指令来手动触发清理。这意味着:

- **只在本地/单人生效**。单人游戏时,客户端和内嵌服务端是同一个 JVM 进程,按键清理的就是整个游戏的内存;如果你只是联机连接到别人的服务器,这个按键只会清理你自己客户端这一侧的内存,不会影响远程服务器。
- 定时自动清理 + 使用率触发清理(带冷却)这两块是在**服务端 tick** 上跑的,专用服务器和单人内嵌服务端都会生效,不受这个限制。

## 功能

- **默认快捷键 J** —— 立即手动执行一次内存清理(可在"控制选项"重新绑定;仅本地/单人生效,见上文)。
- **定时自动清理** —— 默认每 **15 分钟**清理一次,可在配置文件中调整。
- **使用率自动清理** —— 当堆内存使用率达到设定阈值(默认 **80%**)时自动触发清理,但同一冷却周期内(默认 **2 分钟**)只会执行一次。

## ⚠️ 关于"清理内存"的实际效果

清理动作本质上是调用 Java 的 `System.gc()`,只是向 JVM **建议**执行一次垃圾回收,不是强制命令;如果服务器/客户端启动参数里加了 `-XX:+DisableExplicitGC`,这个调用会被直接忽略。频繁强制 GC 有时反而会造成短暂卡顿,所以默认定时间隔 15 分钟、使用率触发冷却 2 分钟,不建议调得过于激进。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | Beta 1.7.3 |
| 加载器 | [Babric](https://babric.github.io/)(Fabric Loader 的 Beta 1.7.3 移植版),`net.fabricmc:fabric-loader` 0.19.3 |
| 前置模组 | [StationAPI](https://github.com/ModificationStation/StationAPI) 2.0.0-alpha.6.2 或更新 |
| Java | 17(开发与运行都建议用 17;这是目前 Babric/StationAPI 生态的标准要求) |

本项目基于 StationAPI 官方贡献者维护的 [stationapi-example-mod](https://github.com/calmilamsy/stationapi-example-mod) 模板搭建,依赖坐标、mappings(`biny`,已锁定当前最新的 `b1.7.3+e0778a3`)、事件系统用法(`GameTickEvent.End`、`KeyBindingRegisterEvent`、`KeyStateChangedEvent`)均对照 StationAPI 真实源码确认过。

## 构建方法

本项目已经包含 Gradle Wrapper,不需要本机预装 Gradle,但需要 **JDK 17**。

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

构建完成后,产物在 `build/libs/memorysweep-1.0.0.jar`。

> 由于本项目开发环境的网络限制,没有条件在联网的真实 Minecraft/Babric 环境中实际编译运行一遍。我本地用 JDK 写了一套匹配真实 API 签名的桩代码(stub)把源码编译了一遍,编译通过,但这终究不能 100% 替代真实环境验证。建议你在本地执行一次 `./gradlew build` 作为最终确认;如果报错,把报错信息发给我,我可以帮你快速定位修正。

### 不想在本地装 JDK 17?用 GitHub Actions 云端构建

项目里已经带了 `.github/workflows/build.yml`。新建一个 GitHub 仓库,把项目推上去,GitHub 会自动在云端完成真正的编译:

```bash
cd memorysweep-babric
git init
git add .
git commit -m "init"
git branch -M main
git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

推送后打开仓库的 **Actions** 标签页,几分钟后在对应运行记录的 **Artifacts** 里下载 `memorysweep-babric-jar`。

⚠️ 如果用网页拖拽上传代码而不是 `git push`,注意 `.github` 这个文件夹名带点,容易被系统文件管理器当作隐藏文件夹漏传。稳妥起见,可以单独用网页的 "Add file → Create new file" 创建 `.github/workflows/build.yml` 并粘贴内容。

## 安装方法

1. 安装 Babric 加载器(参考 [babric.github.io](https://babric.github.io/) 的安装说明)。
2. 下载并安装 [StationAPI](https://github.com/ModificationStation/StationAPI) 2.0.0-alpha.6.2 或更新版本的 jar,放进 `mods` 文件夹。
3. 把构建出来的 `memorysweep-1.0.0.jar` 也放进 `mods` 文件夹。
4. 启动游戏。

## 配置文件说明(`config/memorysweep.properties`)

修改配置文件后需要**重启游戏/服务器**才能生效。

| 字段 | 默认值 | 说明 |
|---|---|---|
| `autoCleanupEnabled` | `true` | 是否启用"定时自动清理" |
| `intervalMinutes` | `15` | 定时自动清理的间隔(分钟) |
| `usageBasedCleanupEnabled` | `true` | 是否启用"根据内存使用率自动清理" |
| `memoryUsageThresholdPercent` | `80` | 触发使用率清理的堆内存占用阈值(百分比,1-99) |
| `usageCheckCooldownSeconds` | `120` | 使用率触发的清理,两次执行之间的最短间隔(秒);默认 2 分钟 |
| `usageCheckIntervalSeconds` | `5` | 后台检查内存使用率的频率(秒) |
| `logToConsole` | `true` | 清理后是否在控制台/日志中输出结果 |

> 注:配置文件的说明注释是英文的 —— 这是因为 Java 的 `Properties` 文件格式会把非 ASCII 字符转成转义序列,中文注释在文本编辑器里会显示成一堆 `\uXXXX`,所以特意用了英文说明,但字段名和你在下面表格里看到的中文说明是一致的。

## 项目结构

```
memorysweep-babric/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/...
├── .github/workflows/build.yml
├── LICENSE
├── README.md
└── src/main/
    ├── java/com/memorysweep/
    │   ├── MemorySweepMod.java         # 核心静态持有者(加载配置、创建 MemoryMonitor)
    │   ├── MemoryMonitor.java          # 清理逻辑本体(System.gc() + 计时)
    │   ├── MemoryCleanupListener.java  # 服务端 tick 监听:定时清理 + 使用率触发清理
    │   ├── KeybindListener.java        # 客户端快捷键监听:手动触发清理
    │   └── config/MemorySweepConfig.java   # Properties 配置读写
    └── resources/
        ├── fabric.mod.json
        └── assets/memorysweep/stationapi/lang/en_US.lang   # 快捷键名称翻译
```

## 个性化

发布前建议编辑 `src/main/resources/fabric.mod.json` 里的 `authors` 字段,填上你自己的名字。如果想换一个默认快捷键,修改 `KeybindListener.java` 里 `Keyboard.KEY_J` 那一行(参考 [LWJGL 2 Keyboard 类的按键常量列表](https://legacy.lwjgl.org/javadoc/org/lwjgl/input/Keyboard.html))。
