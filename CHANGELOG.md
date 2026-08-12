# 更新日志

所有项目的重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.0.6] - 2026-08-12

### 新增功能
- ✨ **Co-Authored-By 智能选择器**: 提交信息追加功能新增专门的 Co-Authored-By 对话框，支持：
  - 📋 预定义 10 个常见 AI 助手列表（Claude Code、Cursor、GitHub Copilot、DeepSeek 等）
  - ✅ 多选支持，一次可添加多个作者
  - ✏️ 自定义输入，支持添加列表中没有的作者
  - 🔄 智能去重，基于邮箱地址自动去重
  - 📝 自动格式化为标准的 `Co-Authored-By: Name <email>` 格式
  - 🔗 多个作者用分号（`;`）分隔，符合 Git 提交消息约定

### 改进
- 🎨 **优化追加内容交互**: Co-Authored-By 类型不再需要输入框，改用对话框选择，交互更直观

### 文档
- 📝 **新增完整功能文档**: 添加 Co-Authored-By 功能说明、快速使用指南、测试清单和实现总结文档

## [1.0.5] - 2026-08-10

### 新增功能
- ✨ **Arthas 热修复支持保存 Class 文件**: 脚本输出对话框新增"保存 Class 文件"按钮，可直接将 `.class` 文件保存到指定目录，方便手动部署或存档
- 📝 **新增简洁版使用说明**: 添加 `使用说明.md`，提供更简洁的快速上手指南

### 改进
- 🎨 **优化 Arthas 对话框按钮文案**: 将"保存到文件"按钮改为"保存脚本"，与新增的"保存 Class 文件"按钮形成清晰区分

### 修复
- 🐛 **OpenAI Codex 模型兼容性修复**: 修复 Codex 模型（code-davinci-002 等）已被 OpenAI 弃用导致的 API 调用失败问题
  - 增强错误提示：当检测到 HTTP 404 错误时，明确提示模型已停止支持并建议迁移到新模型
  - 自动迁移逻辑：插件启动时自动将已弃用的 Codex 模型迁移到 `gpt-4o-mini`
  - 更新配置界面：在设置页面明确标注 Codex 模型已停止支持，推荐使用 gpt-4o-mini、gpt-4o 等新模型

### 文档
- 📝 **更新需求文档**: 补充 Arthas 保存 Class 文件功能的需求说明

## [1.0.4] - 2026-07-25

### 新增功能
- ✨ **可选是否变基**: 提交对话框新增"变基到目标分支"开关，关闭时跳过 fetch/rebase，直接 commit 并使用普通 `push`（而非 `--force-with-lease`）避免误覆盖远程历史。开关默认值从 `GitRebaseSettings` 读取并持久化。当"变基"与"提交MR"均未选中时，目标分支选择器将被禁用。
- ✨ **GitHub PR 自动创建**: 当 origin 远程指向 GitHub 时，推送后可自动通过 GitHub REST API（`POST /repos/{owner}/{repo}/pulls`）创建 Pull Request。首次使用弹框提示输入 Personal Access Token（需 `repo` 权限），Token 安全存储于 `PasswordSafe`；创建前查重已有 open PR，避免重复；任何失败均回退到手动创建链接。
- ✨ **源文件直接生成热更新脚本**: 编辑器右键菜单（`EditorPopupMenu`、`EditorTabPopupMenu`）支持直接对 `.java`/`.kt` 源文件触发 Arthas 热修复脚本生成，无需先手动定位 `.class` 文件；插件自动在常见输出目录中查找对应的编译产物。

### 改进
- 🎨 **Arthas 双版本脚本输出**: 脚本输出对话框新增「完整版」与「剪贴板版」两种格式——完整版含 SHA-256 完整性校验，剪贴板版去除校验逻辑以便快速粘贴执行。
- 🎨 **Arthas 上下文菜单过滤**: 右键菜单仅在选中 `.java`、`.kt` 或 `.class` 文件时显示，避免在无关文件上误触发。
- ⚡ **Arthas 线程模型优化**: `ArthasHotfixAction` 改用 `ActionUpdateThread.BGT` 在后台线程执行 `update()` 检查，减少对 EDT 的占用。

### 技术改进
- 🔒 **Arthas 脚本完整性校验**: 热修复脚本内嵌 SHA-256 哈希，部署时自动与源文件比对，防止上传错误的 `.class` 文件。gzip 压缩后再 Base64 编码，有效减小脚本体积。

## [1.0.3] - 2026-07-23

### 改进
- 🧹 **移除模板示例代码**: 删除插件模板自带的示例 `GitPluginWindowFactory` ToolWindow 及其 `MyMessageBundle` 资源文件,清理无用的工具窗口注册

### 文档
- 📝 **补录更新日志**: 补充 1.0.1、1.0.2 版本的变更记录,使 CHANGELOG 与发布版本保持一致

## [1.0.2] - 2026-07-23

### 改进
- 🎨 **更新插件图标**: 优化 `pluginIcon.svg` 图标样式

### 修复
- 🐛 **移除弃用 API**: 使用 `com.intellij.openapi.ui.ComboBox` 替换弃用的 `JComboBox`,并清理不再建议使用的调用
- 🧹 **代码清理**: 删除废弃的 `ArthasHotfixDialog`,简化对话框逻辑

## [1.0.1] - 2026-07-22

### 新增功能
- ✨ **多模块项目支持**: Arthas 热修复脚本生成支持多模块项目,自动定位各模块的编译输出目录查找 `.class` 文件

## [1.0.0] - 2026-07-22

### 新增功能
- ✨ **智能变基与推送**: 一键完成 fetch → rebase → push 全流程
- ✨ **AI 提交消息生成**: 基于 OpenAI API 自动生成规范的提交消息
- ✨ **Arthas 热修复脚本**: 为 `.class` 文件生成 Arthas 热修复脚本（Base64 编码）
- ✨ **GitLab MR 自动创建**: 推送后自动创建 Merge Request
- ✨ **安全推送**: 使用 `--force-with-lease` 防止覆盖他人提交
- ✨ **国际化支持**: 完整的中英文界面
- ✨ **自动提交变更**: 变基前自动提交所有变更文件
- ✨ **智能分支建议**: 自动识别并推荐 master/main/develop 分支
- ✨ **实时进度显示**: 统一对话框显示操作进度
- ✨ **配置持久化**: 保存用户偏好（默认分支、自动 stash、通知设置）

### 改进
- 🎨 **简化 Arthas 操作**: 直接使用 IDE 选中的 `.class` 文件，无需额外对话框
- 🎨 **统一用户界面**: 变基对话框集成所有配置和进度显示
- 🎨 **批量处理支持**: Arthas 脚本生成支持同时处理多个文件
- 🎨 **规范化命名空间**: 更新插件 ID 及配置项为统一的 `com.examplecn` 命名空间
- 🔒 **安全存储**: GitLab Token 使用系统密钥链存储
- 🔒 **智能 URL 解析**: 支持 SSH/HTTPS 格式及 GitLab 子组

### 修复
- 🐛 **EDT 线程安全**: 所有 Git 操作用 `runReadAction`/`runWriteAction` 包裹
- 🐛 **远程仓库解析**: 修复 GitLab 远程 URL 解析问题
- 🐛 **文件删除处理**: 修复删除文件时的 `git add` 错误，正确使用 `git add -A` 处理所有变更

### 技术栈
- **平台**: IntelliJ Platform 2025.3.5
- **语言**: Kotlin 1.9+
- **构建**: Gradle 8.x
- **JDK**: 17+

### 已知限制
- ⚠️ 多仓库项目仅操作第一个仓库
- ⚠️ 不支持交互式 rebase (`-i`)
