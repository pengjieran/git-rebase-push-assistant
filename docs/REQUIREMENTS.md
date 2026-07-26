# 待实现需求

本文档记录已规划但尚未实现的功能需求。

> 需求 1（允许选择是否变基）与需求 2（Git 操作支持 GitHub）已于 1.0.4 实现，详见 CHANGELOG。
> 注：需求 2 目前按 `github.com` 域名识别 GitHub，自建 GitHub Enterprise 域名尚未覆盖。

## 需求 1：自动生成提交信息支持配置提示词模板

**背景**：`OpenAIService` 目前使用硬编码的中文 prompt 从 git diff 生成 Conventional Commits 提交信息。

**目标**：允许用户在设置中自定义生成提交信息所用的提示词模板。

**要点**：
- 在设置 UI 中新增多行文本框，用于编辑 prompt 模板。
- 支持占位符（如 `{diff}`）在运行时替换为实际 diff 内容。
- 提供合理默认模板；用户留空时回退到默认。
- 模板作为非敏感配置持久化到 `gitRebasePlugin.xml`。

**涉及模块**：`service/OpenAIService`、`config/GitRebaseSettings`、`config/GitRebaseSettingsConfigurable`。

## 需求 2：Arthas 热更新 —— 源文件选中时显示重新编译按钮

**背景**：当用户选中 `.java`/`.kt` 源文件触发热更新脚本生成时，脚本内容依赖已编译的 `.class` 文件。若源文件修改后尚未编译，生成的脚本会基于旧字节码，容易造成误操作。

**目标**：在 `ArthasScriptOutputDialog` 中，当来源是源文件时显示「重新编译」按钮，点击后自动触发 IntelliJ 编译该文件，编译成功后刷新脚本内容。

**要点**：
- `ArthasHotfixAction.generateHotfixScript()` 传入 `sourceFile: VirtualFile?`，仅在源文件路径时非空。
- `ArthasScriptOutputDialog` 新增可选 `sourceFile` 参数；非空时在按钮栏追加「重新编译」按钮。
- 点击后调用 `CompilerManager.getInstance(project).compile(arrayOf(sourceFile), callback)`，编译期间禁用按钮。
- 编译成功（`!aborted && errors == 0`）时重新调用 `ArthasHotfixService.generateHotfixScript(classFile)` 并刷新文本区；失败时提示错误。
- `scriptContent` / `clipboardContent` 改为 `var` 以支持刷新后的复制/保存动作使用最新内容。
- `build.gradle.kts` 中可能需要在 `bundledPlugin` 中补充 `"com.intellij.java"` 依赖（`CompilerManager` 属于 Java 插件模块），需验证。

**涉及模块**：`action/ArthasHotfixAction`、`action/ArthasScriptOutputDialog`、`build.gradle.kts`。