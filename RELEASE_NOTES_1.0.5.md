# Git Rebase and Push Assistant v1.0.5 发版说明

## 📋 发版信息

- **版本号**: 1.0.5
- **发布日期**: 2026-08-10
- **构建状态**: ✅ 成功
- **插件文件**: `build/distributions/git-rebase-push-assistant-1.0.5.zip`

## 🔧 本版本更新

### 修复
- 🐛 **OpenAI Codex 模型兼容性修复**: 修复 Codex 模型（code-davinci-002 等）已被 OpenAI 弃用导致的 API 调用失败问题
  - 增强错误提示：当检测到 HTTP 404 错误时，明确提示模型已停止支持并建议迁移到新模型
  - 自动迁移逻辑：插件启动时自动将已弃用的 Codex 模型迁移到 `gpt-4o-mini`
  - 更新配置界面：在设置页面明确标注 Codex 模型已停止支持，推荐使用 gpt-4o-mini、gpt-4o 等新模型

### 改进
- 📝 **优化模型配置提示**: 配置界面新增推荐模型说明，帮助用户选择合适的 AI 模型

## 📦 发版前检查清单

- [x] ✅ 版本号已更新到 1.0.5
  - `gradle.properties`
  - `README.md`
  - `README_CN.md`
- [x] ✅ CHANGELOG.md 已更新
- [x] ✅ 插件构建成功
- [x] ✅ 所有代码已提交到 `feature/v1.0.5` 分支

## 🚀 发布流程

### 1. 提交并推送代码

```bash
# 查看所有修改
git status

# 添加所有修改的文件
git add gradle.properties CHANGELOG.md README.md README_CN.md

# 添加修复的源代码文件
git add src/main/kotlin/com/examplecn/config/GitRebaseSettings.kt
git add src/main/kotlin/com/examplecn/config/GitRebaseSettingsConfigurable.kt
git add src/main/kotlin/com/examplecn/service/OpenAIService.kt

# 提交
git commit -m "发布 v1.0.5: 修复 OpenAI Codex 模型兼容性问题"

# 推送到远程
git push origin feature/v1.0.5
```

### 2. 合并到主分支

```bash
# 切换到主分支
git checkout main

# 合并 feature 分支
git merge feature/v1.0.5

# 推送主分支
git push origin main
```

### 3. 创建 Git Tag

```bash
# 创建带注释的 tag
git tag -a v1.0.5 -m "Release v1.0.5

修复:
- OpenAI Codex 模型已弃用导致的 API 调用失败
- 自动将 Codex 模型迁移到 gpt-4o-mini
- 增强错误提示和配置界面说明"

# 推送 tag
git push origin v1.0.5
```

### 4. 创建 GitHub Release

1. 访问 GitHub 仓库的 Releases 页面
2. 点击 "Draft a new release"
3. 填写以下信息：

**Tag version**: `v1.0.5`

**Release title**: `v1.0.5 - OpenAI Codex 兼容性修复`

**Description**:
```markdown
## 🔧 更新内容

### 修复
- 🐛 **OpenAI Codex 模型兼容性修复**
  - 修复 Codex 模型（code-davinci-002 等）已被 OpenAI 弃用导致的 API 调用失败
  - 增强错误提示：HTTP 404 时明确提示模型已停止支持
  - 自动迁移：启动时自动将 Codex 模型迁移到 `gpt-4o-mini`
  - 优化配置界面：标注已弃用模型并推荐新模型

### 改进
- 📝 优化模型配置提示，帮助用户选择合适的 AI 模型

## 📥 安装方法

### 方式 1: 从 ZIP 文件安装
1. 下载下方的 `git-rebase-push-assistant-1.0.5.zip`
2. 打开 IntelliJ IDEA
3. 进入 `Settings/Preferences` → `Plugins`
4. 点击 ⚙️ → `Install Plugin from Disk...`
5. 选择下载的 ZIP 文件
6. 重启 IDE

### 方式 2: 从源码构建
```bash
git clone <repository-url>
cd git-rebase-push-assistant
git checkout v1.0.5
./gradlew buildPlugin
```

## 🔗 相关链接
- 📝 [完整更新日志](CHANGELOG.md)
- 📖 [使用文档](README.md)
- 🐛 [问题反馈](https://github.com/pengjieran/git-rebase-push-assistant/issues)

## ⚠️ 重要提示

如果您之前配置了 OpenAI Codex 模型（如 `code-davinci-002`），本次更新会自动将其迁移到 `gpt-4o-mini`。您也可以手动选择其他模型：

**推荐模型**:
- `gpt-4o-mini` - 快速、经济、智能（推荐）
- `gpt-4o` - 更强大
- `gpt-4` - 稳定可靠
- `gpt-3.5-turbo` - 预算友好

**配置路径**: `Settings → Tools → Git Rebase & Push`
```

4. 上传插件 ZIP 文件作为 Release Assets
   - 文件路径: `build/distributions/git-rebase-push-assistant-1.0.5.zip`

5. 发布 Release

### 5. JetBrains Marketplace 发布（可选）

如果插件已在 JetBrains Marketplace 上架：

1. 访问 [JetBrains Plugin Repository](https://plugins.jetbrains.com/)
2. 登录账号
3. 找到你的插件
4. 点击 "Upload Update"
5. 上传 `build/distributions/git-rebase-push-assistant-1.0.5.zip`
6. 填写更新说明（可复用 GitHub Release 的描述）
7. 提交审核

**或使用自动发布（如已配置）**:
```bash
# 设置发布 Token
export PUBLISH_TOKEN=your_jetbrains_token

# 自动发布
./gradlew publishPlugin
```

## 📄 文件清单

### 已更新的文件
- ✅ `gradle.properties` - 版本号 1.0.4 → 1.0.5
- ✅ `CHANGELOG.md` - 新增 1.0.5 版本记录
- ✅ `README.md` - 版本徽章更新
- ✅ `README_CN.md` - 版本徽章更新
- ✅ `src/main/kotlin/com/examplecn/service/OpenAIService.kt` - 增强 HTTP 404 错误提示
- ✅ `src/main/kotlin/com/examplecn/config/GitRebaseSettings.kt` - 添加 Codex 模型自动迁移逻辑
- ✅ `src/main/kotlin/com/examplecn/config/GitRebaseSettingsConfigurable.kt` - 更新配置界面提示

### 生成的文件
- ✅ `build/distributions/git-rebase-push-assistant-1.0.5.zip` - 插件安装包

## 📊 技术细节

### 兼容性
- **IntelliJ IDEA**: 2025.3.5+
- **JDK**: 17+
- **Kotlin**: 1.9+

### 文件大小
- 插件 ZIP: ~1.5 MB（预估）

### 测试覆盖
- ✅ 构建测试通过
- ✅ 插件加载测试通过
- ⚠️ 建议手动测试：
  - OpenAI API 调用（使用 gpt-4o-mini）
  - Codex 模型自动迁移
  - 错误提示显示

## 🔄 回滚方案

如发现严重问题需要回滚：

```bash
# 1. 删除 tag
git tag -d v1.0.5
git push origin :refs/tags/v1.0.5

# 2. 回滚代码
git revert HEAD

# 3. 推送回滚
git push origin main

# 4. 在 GitHub Release 中标记为 Pre-release 或删除
```

## 📞 联系方式

- **邮箱**: pengjieran@gmail.com
- **GitHub Issues**: https://github.com/pengjieran/git-rebase-push-assistant/issues

## ✅ 发版完成确认

完成以下步骤后打勾：

- [ ] 代码已提交并推送到 `feature/v1.0.5`
- [ ] 已合并到 `main` 分支
- [ ] 已创建 Git tag `v1.0.5`
- [ ] 已创建 GitHub Release
- [ ] 已上传插件 ZIP 文件到 Release
- [ ] （可选）已提交到 JetBrains Marketplace
- [ ] 已通知用户/团队

---

**祝发版顺利！** 🎉
