# 待实现需求

本文档记录已规划但尚未实现的功能需求。

## 需求 1：允许选择是否变基到目标分支

**背景**：当前 `UnifiedRebaseDialog` 工作流固定执行 fetch → rebase → commit → force-push，rebase 是强制步骤。

**目标**：在提交对话框中提供开关（如复选框），允许用户选择本次是否变基到目标分支。

**要点**：
- 关闭变基时，跳过 fetch/rebase 步骤，直接 commit → push（此时使用普通 push 而非 `--force-with-lease`，避免误覆盖）。
- 开关默认值可从 `GitRebaseSettings` 读取并持久化。
- 关闭变基时目标分支选择器可禁用。

**涉及模块**：`action/`（对话框 UI）、`service/GitRebaseService`、`config/GitRebaseSettings`。

## 需求 2：Git 操作支持 GitHub

**背景**：当前 MR 自动创建仅支持 GitLab（`MergeRequestService` POST 到 `/api/v4/...`），GitHub PR 自动创建未实现，仅返回手动创建链接。

**目标**：git 操作（尤其是创建 PR/MR）同时支持 GitHub。

**要点**：
- 解析 remote URL 时识别 GitHub 域名（`github.com` 及自建 GitHub Enterprise）。
- 调用 GitHub REST API `POST /repos/{owner}/{repo}/pulls` 创建 PR。
- Token 与 GitLab 一样存入 `PasswordSafe`，配置项区分平台。
- 根据 remote 类型自动路由到 GitLab 或 GitHub 实现。

**涉及模块**：`service/MergeRequestService`（或抽象出平台接口）、`config/GitRebaseSettings`、`config/GitRebaseSettingsConfigurable`。

## 需求 3：自动生成提交信息支持配置提示词模板

**背景**：`OpenAIService` 目前使用硬编码的中文 prompt 从 git diff 生成 Conventional Commits 提交信息。

**目标**：允许用户在设置中自定义生成提交信息所用的提示词模板。

**要点**：
- 在设置 UI 中新增多行文本框，用于编辑 prompt 模板。
- 支持占位符（如 `{diff}`）在运行时替换为实际 diff 内容。
- 提供合理默认模板；用户留空时回退到默认。
- 模板作为非敏感配置持久化到 `gitRebasePlugin.xml`。

**涉及模块**：`service/OpenAIService`、`config/GitRebaseSettings`、`config/GitRebaseSettingsConfigurable`。