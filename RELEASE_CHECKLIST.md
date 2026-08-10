# v1.0.5 发版快速清单

## ✅ 已完成

- [x] 版本号更新到 1.0.5 (gradle.properties, README.md, README_CN.md)
- [x] CHANGELOG.md 更新
- [x] 代码修复完成（OpenAI Codex 兼容性 + Arthas Class 文件保存）
- [x] 插件构建成功: `git-rebase-push-assistant-1.0.5.zip` (107KB)

## 📋 待执行

### 1️⃣ 提交代码
```bash
cd C:\sources\github\git-rebase-push-assistant

# 查看修改
git status

# 添加文件
git add .

# 提交
git commit -m "发布 v1.0.5: Arthas 增强与 OpenAI Codex 兼容性修复

新增功能:
- Arthas 热修复支持保存 Class 文件
- 添加简洁版使用说明文档

改进:
- 优化 Arthas 对话框按钮文案

修复:
- 修复 OpenAI Codex 模型已弃用导致的 API 调用失败
- 自动迁移 Codex 模型到 gpt-4o-mini
- 增强 HTTP 404 错误提示"

# 推送
git push origin feature/v1.0.5
```

### 2️⃣ 合并到主分支
```bash
git checkout main
git merge feature/v1.0.5
git push origin main
```

### 3️⃣ 创建 Git Tag
```bash
git tag -a v1.0.5 -m "Release v1.0.5 - Arthas 增强与 OpenAI Codex 兼容性修复"
git push origin v1.0.5
```

### 4️⃣ 创建 GitHub Release

1. 访问: https://github.com/pengjieran/git-rebase-push-assistant/releases/new
2. Tag: `v1.0.5`
3. Title: `v1.0.5 - Arthas 增强与 OpenAI Codex 兼容性修复`
4. 上传文件: `build/distributions/git-rebase-push-assistant-1.0.5.zip`
5. 复制以下内容作为描述:

---

## 🔧 更新内容

### 新增功能
- ✨ **Arthas 热修复支持保存 Class 文件**
  - 脚本输出对话框新增"保存 Class 文件"按钮
  - 可直接将 `.class` 文件保存到指定目录，方便手动部署或存档
- 📝 **新增简洁版使用说明**: 添加 `使用说明.md`，提供更简洁的快速上手指南

### 改进
- 🎨 **优化 Arthas 对话框按钮文案**: 将"保存到文件"改为"保存脚本"，与"保存 Class 文件"形成清晰区分

### 修复
- 🐛 **OpenAI Codex 模型兼容性修复**
  - 修复 Codex 模型（code-davinci-002 等）已被 OpenAI 弃用导致的 API 调用失败
  - 增强错误提示：HTTP 404 时明确提示模型已停止支持
  - 自动迁移：启动时自动将 Codex 模型迁移到 `gpt-4o-mini`
  - 优化配置界面：标注已弃用模型并推荐新模型

## ⚠️ 重要提示

如果您之前配置了 OpenAI Codex 模型（如 `code-davinci-002`），本次更新会自动将其迁移到 `gpt-4o-mini`。

**推荐模型**:
- `gpt-4o-mini` - 快速、经济、智能（推荐）
- `gpt-4o` - 更强大
- `gpt-4` - 稳定可靠
- `gpt-3.5-turbo` - 预算友好

**配置路径**: `Settings → Tools → Git Rebase & Push`

## 📥 安装

下载 `git-rebase-push-assistant-1.0.5.zip` → IntelliJ IDEA → Settings → Plugins → ⚙️ → Install Plugin from Disk

---

## 🎯 关键信息

- **版本**: 1.0.5
- **日期**: 2026-08-10
- **文件**: git-rebase-push-assistant-1.0.5.zip (107KB)
- **主要更新**: Arthas Class 文件保存 + OpenAI Codex 兼容性修复
- **兼容性**: IntelliJ IDEA 2025.3.5+

## 📞 需要帮助?

- 📧 pengjieran@gmail.com
- 🐛 [GitHub Issues](https://github.com/pengjieran/git-rebase-push-assistant/issues)
