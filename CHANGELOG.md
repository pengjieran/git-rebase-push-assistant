# 更新日志

所有项目的重要变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

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
- ⚠️ GitHub PR 自动创建尚未实现
- ⚠️ 不支持交互式 rebase (`-i`)
