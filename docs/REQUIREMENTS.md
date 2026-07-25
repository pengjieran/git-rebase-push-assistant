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