
# 快速开始指南

## 5分钟上手Git变基推送插件

### 第1步：安装插件

```bash
# 构建插件
cd /Users/apple/sources/git-plugin
./gradlew buildPlugin

# 生成的插件位于
# build/distributions/git-plugin-1.0.0-SNAPSHOT.zip
```

在IDEA中安装：
1. `Preferences` → `Plugins`
2. 点击 ⚙️ → `Install Plugin from Disk...`
3. 选择生成的 ZIP 文件
4. 重启IDEA

### 第2步：打开Git项目

打开任意Git项目，确保：
- 有远程仓库 (origin)
- 有多个分支
- 当前在某个开发分支上

### 第3步：使用功能

**方法1: 从提交窗口**
1. 按 `Cmd+K` (macOS) 或 `Ctrl+K` (Windows/Linux) 打开提交窗口
2. 在提交按钮旁边找到 "Rebase and Push" 按钮
3. 点击按钮

**方法2: 从VCS菜单**
1. 菜单栏 → `Git` → `Rebase and Push...`

### 第4步：配置变基

在弹出的对话框中：

```
┌────────────────────────────────────────┐
│  Rebase and Push Configuration         │
├────────────────────────────────────────┤
│  Target Branch:  [master ▼]            │
│                                        │
│  ☐ Create Merge Request after push    │
│                                        │
│  [Cancel]              [OK]            │
└────────────────────────────────────────┘
```

1. **选择目标分支**: 下拉选择要变基到的分支
2. **MR选项**: 可选勾选是否创建Merge Request
3. 点击 `OK`

### 第5步：查看结果

插件会显示进度：
```
Rebasing feature-login onto master
├─ Fetching remote branch master... ▓▓░░░░ 20%
├─ Rebasing feature-login onto master... ▓▓▓░░░ 50%
└─ Pushing to remote... ▓▓▓▓▓░ 80%
```

完成后显示：
```
✓ Successfully rebased feature-login onto master 
  and pushed to remote.
```

## 常见使用场景

### 场景A: 同步主干最新代码

```bash
# 你的情况
当前分支: feature-new-ui
主干: master (比你的分支新了10个提交)

# 操作
1. 点击 "Rebase and Push"
2. 选择 master
3. OK

# 结果
feature-new-ui 现在包含 master 的最新代码
```

### 场景B: 准备提交PR
### 场景B: 准备提交PR

```bash
# 你的情况
开发完成，准备提交Pull Request到develop分支

# 操作
1. 点击 "Rebase and Push"
2. 选择 develop
3. 勾选 "Create Merge Request after push"
4. OK

# 结果
- 分支已变基到最新的develop
- 已推送到远程
- 显示创建MR的提示
```

### 场景C: 整理提交历史

```bash
# 你的情况
feature分支有很多零碎提交，想要基于最新master重新整理

# 操作
1. 点击 "Rebase and Push"
2. 选择 master
3. OK

# 结果
提交历史变得线性，基于最新master
```

## 问题排查

### 问题：找不到按钮

**检查清单**：
- [ ] 插件已安装？
- [ ] IDEA已重启？
- [ ] 当前是Git项目？
- [ ] 有远程仓库？

**解决**：
```bash
# 验证Git状态
git remote -v
git branch -a
```

### 问题：变基失败

**常见原因**：冲突

**解决**：
```bash
# 查看状态
git status

# 解决冲突后
git add <resolved-files>
git rebase --continue

# 或放弃
git rebase --abort
```

### 问题：推送失败 "lease failed"

**原因**：远程有新提交

**解决**：
```bash
# 重新拉取
git fetch origin

# 使用插件重新变基
```

## 下一步

- 📖 阅读 [USAGE_GUIDE.md](USAGE_GUIDE.md) 了解详细用法
- 🏗️ 查看 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) 了解项目结构
- 🔧 阅读 [README_REBASE.md](README_REBASE.md) 了解技术细节

## 快捷键（可自定义）

在IDEA设置中搜索 "Rebase and Push" 可以设置快捷键：

```
Preferences → Keymap → 搜索 "Rebase and Push"
→ 右键 → Add Keyboard Shortcut
```

建议快捷键：
- macOS: `Cmd+Shift+R`
- Windows/Linux: `Ctrl+Shift+R`

## 小贴士

💡 **智能分支建议**
插件会自动建议常见分支：
- 优先显示 `master` / `main` / `develop`
- 自动排除当前分支

💡 **安全推送**
使用 `--force-with-lease` 而不是 `--force`，更安全！

💡 **后台执行**
所有Git操作在后台执行，不会卡住界面

💡 **错误友好**
清晰的错误提示，帮助快速定位问题

## 获得帮助

- 📧 提交Issue
- 📚 查看完整文档
- 💬 联系开发团队

祝使用愉快！🎉
