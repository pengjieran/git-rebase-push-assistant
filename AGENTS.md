# AGENTS.md

面向 AI 编码代理的仓库工作指南。完整架构说明见 `CLAUDE.md`，本文仅作为文件目录索引说明，告诉AI每个目录的路径和用途。

## 项目是什么

一个用 Kotlin 编写的 IntelliJ IDEA 插件（插件 id：`io.github.pengjran.git-rebase-push-assistant`）。两大功能：

1. **Git 变基与推送** —— 在 Git 提交对话框中提供「变基并提交推送」操作，依次执行 fetch → rebase → commit → 强制推送（`--force-with-lease`）→ 可选创建 GitLab MR。变基可开关：关闭时跳过 fetch/rebase，改用普通 `push`。
2. **Arthas 热修复生成器** —— 在 `.java`/`.kt`/`.class` 文件上右键，生成 Base64 编码的 bash 脚本，用于阿里 Arthas 生产热修复。

## 环境与命令

- 需要 **JDK 17+**，目标平台为 IntelliJ IDEA 2025.3.5。
- `./gradlew compileKotlin` —— 快速编译检查；每次改代码后先跑它。
- `./gradlew buildPlugin` —— 构建可分发包，产物在 `build/distributions/git-plugin-*.zip`。
- `./gradlew runIde` —— 启动装好插件的沙箱 IDE。
- `./gradlew test` —— 运行测试（通过 `check` 生命周期）。**目前没有测试** —— `src/test/kotlin` 为空。
- `./gradlew verifyPlugin` —— 校验与目标 IntelliJ 版本的兼容性。

预置的运行/调试配置在 `.run/` 下（Run Plugin、Run Tests、Run Verifications）。

## 目录结构

包根路径：`src/main/kotlin/com/examplecn/`

- `action/` —— UI 入口：`GitRebaseAndPushAction`（提交对话框，注册到 `Vcs.Commit.PrimaryCommitActions`）、`ArthasHotfixAction`（Tools 菜单 / 项目视图 / 编辑器右键），以及对应对话框（`UnifiedRebaseDialog`、`ArthasScriptOutputDialog`）。
- `service/` —— 四个 `@Service(Service.Level.PROJECT)` 服务：`GitRebaseService`、`OpenAIService`、`MergeRequestService`、`ArthasHotfixService`。
- `config/` —— `GitRebaseSettings`（`PersistentStateComponent`，持久化到 `gitRebasePlugin.xml`）与 `GitRebaseSettingsConfigurable`（设置界面，位于 Tools > Git Rebase & Push）。
- `bundle/` —— `GitRebaseBundle`，负责 i18n 文案查找。
- `src/main/resources/messages/` —— `GitRebaseBundle.properties` 及 `_zh_CN` 中文变体。
- `src/main/resources/META-INF/plugin.xml` —— 插件描述文件；新增 action/扩展点在此注册。

## 代码约定

- **Kotlin，4 空格缩进**，与周围文件风格保持一致。
- **KDoc 注释用中文** —— 新增文档注释请沿用中文。
- Git 操作统一封装 Git4Idea 的 `GitLineHandler` + `Git.getInstance().runCommand(...)`；检查 `result.success()`，失败时抛 `VcsException` 并带上 `result.errorOutputAsJoinedString`。
- 面向用户的文案一律走 `GitRebaseBundle.message("key")`，两个 `.properties` 文件都要有对应条目 —— 禁止硬编码 UI 文本。
- **无外部运行时依赖**。JSON 全部用字符串手工拼接与解析，不要往插件 classpath 引入库。
- 密钥（GitLab PAT）存在 IntelliJ `PasswordSafe` 中，绝不放进 `GitRebaseSettings` 或持久化 XML。

## 线程模型

所有 Git4Idea 调用都在 **EDT 上同步执行**，包裹在 `ApplicationManager.getApplication().run{Read,Write}Action(Computable {...})` 中 —— 读操作用 read action，写操作用 write action。没有后台协程，也没有 `ProgressManager` 任务。改动 `GitRebaseService` 或对话框流程时务必保持这一模型。

## Git 安全

- 除非明确要求，只推送到新分支，绝不直推 `main`/`master`。主分支为 `main`。
- 仅在用户明确要求时提交。优先新建提交，而非 `--amend`。
- 未经明确许可，绝不执行破坏性 git 命令（`reset --hard`、`clean -f`、强制推送、`branch -D`）。

## 易踩的坑

- 多仓库项目**不做消歧** —— 代码始终使用 `repositories.firstOrNull()`。
- GitLab MR 与 GitHub PR 均支持自动创建（按 origin 远程 URL 自动检测平台）；缺 Token 时弹框提示输入，失败回退手动链接。
- 当前版本为 **1.0.4**（`gradle.properties`），与 CHANGELOG 最新条目一致。
- 新增功能或修 bug 时，请在 `src/test/kotlin` 下建立首批测试，不要假设已有测试框架就绪。
