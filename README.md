# Git Rebase and Push Plugin

[![Twitter Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=twitter)](https://twitter.com/JBPlatform)
[![Developers Forum](https://img.shields.io/badge/JetBrains%20Platform-Join-blue)][jb:forum]

IntelliJ IDEA插件，在Git提交页面添加智能变基和推送功能。

## ✨ 主要特性

- 🎯 **一键变基**: 选择目标分支，自动完成 fetch → rebase → push
- 🔒 **安全推送**: 使用 `--force-with-lease` 防止覆盖他人提交
- 🚀 **后台执行**: 所有Git操作在后台线程执行，不阻塞UI
- ✨ **自动创建MR**: 通过GitLab API自动创建Merge Request（首次使用需配置Token）
- 📝 **可选提交**: 推送前可选择变动文件并提交
- 🎨 **智能建议**: 自动识别并推荐 master/main/develop 分支
- ⚡ **进度提示**: 实时显示操作进度和友好的错误信息

## 📥 快速开始

### 安装

```bash
# 克隆项目
git clone <repository-url>
cd git-plugin

# 构建插件
./gradlew buildPlugin

# 生成的插件位于: build/distributions/git-plugin-1.0.0-SNAPSHOT.zip
```

在IDEA中安装：
1. `Preferences/Settings` → `Plugins`
2. 点击 ⚙️ → `Install Plugin from Disk...`
3. 选择生成的 ZIP 文件
4. 重启IDEA

详细步骤请查看 [QUICK_START.md](QUICK_START.md)

## 🎯 使用方法

### 基本用法

1. 打开Git提交窗口（`Cmd+K` / `Ctrl+K`）
2. 点击 **"变基并提交"** 按钮
3. 选择目标分支
4. 可选勾选 "Create Merge Request after push"
5. 点击 OK

### 使用场景示例

#### 将feature分支同步到最新master

```bash
# 当前分支: feature/new-login
# 目标: 同步master最新代码

1. 点击 "Rebase and Push"
2. 选择: master
3. OK
```

插件执行：
```bash
git fetch origin master
git rebase origin/master
git push --force-with-lease origin feature/new-login
```

更多使用场景请查看 [USAGE_GUIDE.md](USAGE_GUIDE.md)

## 📚 文档

- 📖 [快速开始指南](QUICK_START.md) - 5分钟上手
- 📖 [详细使用指南](USAGE_GUIDE.md) - 完整功能说明
- 📖 [功能说明](README_REBASE.md) - 技术特性
- 📖 [项目结构](PROJECT_STRUCTURE.md) - 代码架构
- 📖 [实现总结](IMPLEMENTATION_SUMMARY.md) - 开发文档

## 🏗️ 项目结构

```
.
├── .run/                           Predefined Run/Debug Configurations
├── build/                          Output build directory
├── gradle                          Gradle Wrapper & configuration
├── src
│   ├── main
│   │   ├── kotlin/com/examplecn/
│   │   │   ├── action/            Action实现 (UI交互)
│   │   │   │   └── GitRebaseAndPushAction.kt
│   │   │   ├── service/           Service层 (业务逻辑)
│   │   │   │   └── GitRebaseService.kt
│   │   │   ├── config/            配置管理
│   │   │   │   └── GitRebaseSettings.kt
│   │   │   └── ...
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml     插件配置
│   └── test/
│       └── kotlin/                单元测试
├── build.gradle.kts               Gradle build configuration
├── QUICK_START.md                 快速开始指南
├── USAGE_GUIDE.md                 使用指南
└── README.md                      This file
```

## 🛠️ 技术栈

- **语言**: Kotlin
- **平台**: IntelliJ Platform 2025.3.5
- **依赖**: Git4Idea (IDEA内置)
- **构建**: Gradle 8.x
- **JDK**: 17+

## 🧪 开发

### 运行测试IDE

```bash
./gradlew runIde
```

这会启动一个包含插件的IDEA测试实例。

### 运行测试

```bash
./gradlew test
```

### 构建插件

```bash
./gradlew buildPlugin
```

生成的插件：`build/distributions/git-plugin-*.zip`

## 🔮 未来计划

- [ ] 集成GitLab API自动创建Merge Request
- [ ] 集成GitHub API自动创建Pull Request
- [ ] 支持批量变基多个分支
- [ ] 智能冲突解决提示
- [ ] 自定义Git命令参数
- [ ] 支持Gitee等其他平台

## 🐛 已知限制

- MR创建目前仅显示提示，需手动在平台创建
- 不支持批量操作
- 冲突需要手动解决
- 配置存储为明文

## 📄 许可证

根据项目需要设置。

---

## Predefined Run/Debug configurations

A generated project contains the following content structure:

```
.
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── gradle
│   ├── wrapper/            Gradle Wrapper
│   ├── libs.versions.toml  Version catalog
├── src                     Plugin sources
│   ├── main
│   │   ├── kotlin/         Kotlin production sources
│   │   └── resources/      Resources - plugin.xml, icons, messages
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Gradle build configuration
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation and the manifest for our plugin – [plugin.xml][file:plugin.xml].

> [!NOTE]
> To use Java in your plugin, create the `/src/main/java` directory.

## Plugin configuration file

The plugin configuration file is a [plugin.xml][file:plugin.xml] file located in the `src/main/resources/META-INF` directory.
It provides general information about the plugin, its dependencies, extensions, and listeners.

You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

If you're still not quite sure what this is all about, read [Introduction to IntelliJ Platform][docs:intro].

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug configurations* that expose corresponding Gradle tasks:

| Configuration name | Description                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin         | Runs [`:runIde`][gh:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests          | Runs [`:check`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                |
| Run Verifications  | Runs [`:verifyPlugin`][gh:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses the `publishPlugin` Gradle task provided by the [intellij-platform-gradle-plugin][gh:intellij-platform-gradle-plugin-docs].

You can also upload the plugin to the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/upload) manually via UI.

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][gh:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]
- [JetBrains Marketplace Paid Plugins][jb:paid-plugins]
- [IntelliJ SDK Code Samples][gh:code-samples]

[docs]: https://plugins.jetbrains.com/docs/intellij
[docs:intro]: https://plugins.jetbrains.com/docs/intellij/intellij-platform.html?from=IJPluginTemplate
[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate
[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples
[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin
[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
[gh:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde
[gh:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md
[jb:forum]: https://platform.jetbrains.com/
[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html
[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html
[jb:ipe]: https://jb.gg/ipe
[jb:ui-guidelines]: https://jetbrains.github.io/ui
