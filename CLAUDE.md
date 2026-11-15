# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作时提供指导。

## 项目概述

一个 IntelliJ IDEA 插件（Kotlin 编写），包含两大功能：
1. **Git 变基与推送** —— 在 Git 提交界面新增「变基并提交推送」操作，自动执行 fetch → rebase → commit → 强制推送（`--force-with-lease`）→ 可选创建 GitLab MR。变基可开关：当「变基到目标分支」开关关闭时，跳过 fetch/rebase，改用普通 `push`（而非 `--force-with-lease`）。
2. **Arthas 热修复生成器** —— 在 `.java`/`.kt`/`.class` 文件上的右键操作，生成 Base64 编码的 shell 脚本，用于阿里 Arthas 生产环境热修复。

## 构建与开发命令

```bash
./gradlew buildPlugin      # 构建插件；产物在 build/distributions/git-plugin-*.zip
./gradlew runIde            # 启动装有该插件的沙箱 IDE 实例
./gradlew test              # 运行测试（通过 `check` 生命周期任务）
./gradlew verifyPlugin      # 校验插件与目标 IntelliJ 版本的兼容性
```

`.run/` 下预置了运行/调试配置（Run Plugin、Run Tests、Run Verifications），封装了相同的 Gradle 任务。

需要 JDK 17+。目标平台为 IntelliJ IDEA 2025.3.5，在 `build.gradle.kts` 中通过 `intellijIdea("2025.3.5")` 声明，并以 `Git4Idea` 作为捆绑插件依赖。

## 构建状态

项目可正常编译和构建。`src/test/kotlin` 为空 —— 尚无任何测试。

**GitHub PR** —— 已支持自动创建：通过 GitHub REST API（`POST /repos/{owner}/{repo}/pulls`）创建 PR，Token 存储在 `PasswordSafe`；创建前会查重已有 open PR，失败时回退到手动创建链接。

## 架构

包根路径：`src/main/kotlin/com/examplecn/`

**`action/`** —— UI 入口（两个已注册的 action）：
- `GitRebaseAndPushAction` —— 出现在 Git 提交对话框（`Vcs.Commit.PrimaryCommitActions` 组）。仅当项目至少有一个 Git 仓库时启用。始终操作 `repositories.firstOrNull()` —— 多仓库项目不做消歧。先在 `Task.Backgroundable` 中预加载变更文件与远程分支，再在 `onSuccess`（EDT）中打开 `UnifiedRebaseDialog`，该对话框整合了全部配置。用户确认后，在另一个 `Task.Backgroundable` 中执行：add + commit → fetch → rebase → 强制推送 → 可选创建 MR，进度显示在后台任务指示器中。提交消息通过 `VcsDataKeys.COMMIT_MESSAGE_DOCUMENT` 读取。
- `ArthasHotfixAction` —— 出现在 `ToolsMenu`、`ProjectViewPopupMenu`、`EditorPopupMenu` 和 `EditorTabPopupMenu` 中。接受 `.java`、`.kt` 或 `.class` 文件选择。对于源文件，会在常见输出目录（`target/classes`、`build/classes/kotlin/main`、`out/production/…`）中搜索定位对应的已编译 `.class`。委托给 `ArthasHotfixService`，随后弹出带复制/保存选项的 `ArthasScriptOutputDialog`。

**`service/`** —— 四个项目级服务（`@Service(Service.Level.PROJECT)`）：
- `GitRebaseService` —— 封装所有 Git4Idea 的 `GitLineHandler` 调用（fetch、rebase、push、status、add、commit、remote 查询）。`GitLineHandler` 调用会阻塞，必须在后台线程执行；`GitRepository` 的 getter（分支、remote）读取的是 volatile 不可变快照，可在任意线程直接调用，无需 read action。
- `OpenAIService` —— 使用 `HttpURLConnection`（无外部库）调用 OpenAI 兼容的 chat completions API。基于 git diff 构建提示词，生成中文的规范化提交消息。
- `MergeRequestService` —— 根据 origin 远程 URL 检测平台（含 `github.com` 判为 GitHub，否则 GitLab）。GitLab：向 `/api/v4/projects/{project_path}/merge_requests` 发起 POST，解析远程 URL（SSH 与 HTTPS，含子组）推导基础 URL 和项目路径。GitHub：向 `https://api.github.com/repos/{owner}/{repo}/pulls` 发起 POST。两者均先查重已有 open MR/PR，缺 Token 时弹框提示输入，令牌分别以 `GitLabToken`/`GitHubToken` 存储在 IntelliJ 的 `PasswordSafe`（系统钥匙串）中，任何失败都回退到手动创建链接。
- `ArthasHotfixService` —— 读取 `.class` 文件，先 gzip 压缩再 Base64 编码（MIME，76 列换行），生成一个自包含的 bash 脚本：解码并 `gunzip` 到 `/tmp`，对照源文件 SHA-256 校验（`sha256sum`/`shasum` 回退），最后打印 Arthas 的 `retransform` 命令。

**`config/`** —— `GitRebaseSettings`（`PersistentStateComponent`）将非机密偏好（默认目标分支、autostash、成功时通知、OpenAI 端点/模型、非机密 GitLab 配置）持久化到 `gitRebasePlugin.xml`。`GitRebaseSettingsConfigurable` 在 `Tools > Git Rebase & Push` 下渲染设置界面。

**`bundle/`** —— `GitRebaseBundle` 封装 `ResourceBundle` 查找。消息属性文件位于 `src/main/resources/messages/`，并带有用于中文本地化的 `_zh_CN` 变体。

**线程模型：** 耗时操作（git 命令、HTTP 请求）放在 `ProgressManager` + `Task.Backgroundable`（或 `executeOnPooledThread`）中，结果在 `onSuccess`/`onThrowable` 或 `invokeLater(..., ModalityState)` 中回到 EDT 更新 UI。不要使用 `SwingUtilities.invokeLater`、裸 `Thread`，也不要用 `runReadAction { }` / `ReadAction.compute`（2026.1+ 已弃用）；确需读锁时用 `ReadAction.nonBlocking(...)`。`GitRebaseBundle` 通过委托持有 `DynamicBundle(Class, String)` 实例，不要继承 `DynamicBundle`（2026.3 起弃用）。Action 必须重写 `getActionUpdateThread()`（当前均为 `BGT`），不得依赖已弃用的默认值 `OLD_EDT`。

**无外部运行时依赖** —— JSON 的构造与解析均通过字符串操作手工完成，以避免向插件 classpath 引入库。
