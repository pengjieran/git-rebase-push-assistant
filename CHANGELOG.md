<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Git-plugin Changelog

## [Unreleased]

## [1.0.0] - 2026-07-17

### Added
- 一键变基并推送:自动 fetch 目标分支、rebase、force-push (带 `--force-with-lease` 保护)
- 自动创建 GitLab Merge Request(首次使用提示输入 Personal Access Token,安全存储到系统凭据管理器)
- 支持 JIRA 号和 webhook 标记自动集成到 commit message
- 统一对话框:集成目标分支选择、文件列表、commit message 输入和进度显示
- 自动关闭:操作完成 2 秒后自动关闭对话框
- 用户偏好持久化:默认目标分支、autostash 选项、成功通知开关

### Fixed
- EDT 线程安全:所有 Git 操作都用 `runReadAction`/`runWriteAction` 包裹,避免线程冲突
- GitLab 远程 URL 解析支持 SSH/HTTPS 格式及 subgroup 路径
