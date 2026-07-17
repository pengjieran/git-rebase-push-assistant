# Git变基推送插件使用指南

## 快速开始

### 安装插件

1. 构建插件：
   ```bash
   ./gradlew buildPlugin
   ```

2. 安装到IDEA：
   - 打开IDEA设置 (Preferences/Settings)
   - 选择 Plugins → ⚙️ → Install Plugin from Disk...
   - 选择 `build/distributions/git-plugin-*.zip`
   - 重启IDEA

### 使用功能

#### 场景1：将feature分支变基到master

假设您正在 `feature/new-login` 分支上开发：

1. 打开Git提交窗口（Ctrl+K / Cmd+K）
2. 点击"Rebase and Push"按钮
3. 在对话框中：
   - 选择目标分支：`master`
   - 可选勾选"Create Merge Request after push"
4. 点击OK

插件将执行：
```bash
git fetch origin master
git rebase origin/master
git push --force-with-lease origin feature/new-login
```

#### 场景2：将开发分支更新到最新主干

假设您的分支落后于主干，需要同步最新代码：

1. 确保当前分支的更改已提交
2. 点击"Rebase and Push"
3. 选择 `main` 或 `develop`作为目标分支
4. 执行后，您的分支将包含主干的最新提交

## 功能详解

### 变基流程

```
┌─────────────────┐
│ 1. Fetch Remote │  拉取目标分支最新内容
└────────┬────────┘
         │
┌────────▼────────┐
│  2. Rebase      │  将当前分支变基到目标分支
└────────┬────────┘
         │
┌────────▼────────┐
│  3. Force Push  │  使用 --force-with-lease 推送
└────────┬────────┘
         │
┌────────▼────────┐
│ 4. Create MR    │  提示创建Merge Request（可选）
└─────────────────┘
```

### 安全保护

#### --force-with-lease

插件使用 `--force-with-lease` 而不是 `--force`：

- ✅ **安全**：如果远程分支有其他人推送的新提交，推送会失败
- ✅ **保护协作**：防止覆盖团队成员的工作
- ❌ **普通force**：会直接覆盖远程分支，可能丢失他人的提交

示例：
```bash
# 使用 --force-with-lease（安全）
git push --force-with-lease origin feature-branch

# 使用 --force（危险）
git push --force origin feature-branch
```

#### 其他安全检查

- 不允许变基到当前分支
- 变基前确保有当前分支信息
- 所有Git操作都有错误处理

## 常见问题

### Q1: 变基失败怎么办？

**场景**：变基过程中出现冲突

**解决方案**：
1. 插件会显示错误信息
2. 在终端手动解决冲突：
   ```bash
   # 查看冲突文件
   git status
   
   # 解决冲突后
   git add <resolved-files>
   git rebase --continue
   
   # 或者放弃变基
   git rebase --abort
   ```

### Q2: 推送失败提示 "lease failed"

**原因**：远程分支有新的提交（可能是其他人推送的）

**解决方案**：
```bash
# 拉取最新内容
git fetch origin

# 重新进行变基
# 或者使用插件重新执行
```

### Q3: 如何创建Merge Request？

**当前版本**：插件显示提示信息，需要手动创建MR

**手动步骤**：
1. **GitLab**：访问项目页面 → Merge Requests → New merge request
2. **GitHub**：访问仓库页面 → Pull requests → New pull request
3. **Gitee**：访问仓库页面 → Pull Requests → 新建 Pull Request

**未来计划**：集成GitLab/GitHub API实现自动创建

### Q4: 可以批量变基多个分支吗？

**当前版本**：不支持批量操作

**替代方案**：
```bash
# 编写脚本批量处理
for branch in feature-1 feature-2 feature-3; do
  git checkout $branch
  git fetch origin master
  git rebase origin/master
  git push --force-with-lease origin $branch
done
```

## 最佳实践

### 1. 变基前的准备

✅ **推荐做法**：
- 提交所有本地更改
- 确保工作区干净（`git status` 无未跟踪文件）
- 与团队沟通（如果是共享分支）

❌ **避免**：
- 在有未提交更改时变基
- 对已发布的公共分支频繁变基
- 多人协作的分支强制推送

### 2. 何时使用变基

**适合场景**：
- ✅ 个人开发分支同步主干
- ✅ Feature分支准备合并前的整理
- ✅ 保持提交历史线性

**不适合场景**：
- ❌ 已经发布的公共分支
- ❌ 多人正在协作的分支
- ❌ 不熟悉Git变基的团队

### 3. 与Merge的对比

| 特性 | Rebase | Merge |
|------|--------|-------|
| 历史记录 | 线性，清晰 | 有分支，保留原始历史 |
| 冲突解决 | 逐个提交解决 | 一次性解决 |
| 共享分支 | 不建议 | 推荐 |
| 提交哈希 | 会改变 | 不改变 |

## 故障排除

### 问题1：找不到"Rebase and Push"按钮

**检查**：
1. 确认插件已安装并启用
2. 检查是否在Git仓库中
3. 重启IDEA

### 问题2：提示"No Git repository found"

**原因**：当前项目不是Git仓库

**解决**：
```bash
cd your-project
git init
```

### 问题3：远程分支列表为空

**原因**：没有远程仓库或未fetch

**解决**：
```bash
git remote add origin <remote-url>
git fetch origin
```

### 问题4：插件构建失败

**检查**：
1. Java版本（需要JDK 17+）
2. Gradle版本
3. 依赖是否正确下载

```bash
./gradlew clean build --refresh-dependencies
```

## 高级配置

### 自定义变基行为

如需修改变基逻辑，可编辑 `GitRebaseService.kt`：

```kotlin
// 修改rebase参数
fun rebaseOnto(repository: GitRepository, targetBranch: String) {
    val handler = GitLineHandler(project, repository.root, GitCommand.REBASE)
    handler.addParameters("origin/$targetBranch")
    // 添加自定义参数，如：
    // handler.addParameters("--autostash")  // 自动暂存
    // handler.addParameters("--strategy=ours")  // 使用特定策略
    val result = Git.getInstance().runCommand(handler)
    // ...
}
```

### 集成GitLab API（示例）

未来版本可以添加自动创建MR的功能：

```kotlin
private fun createGitLabMR(
    project: Project,
    sourceBranch: String,
    targetBranch: String
) {
    // 使用GitLab API创建MR
    val gitlabUrl = "https://gitlab.com"
    val projectId = "your-project-id"
    val privateToken = "your-token"
    
    // POST /api/v4/projects/:id/merge_requests
    // ...
}
```

## 版本历史

- **v1.0.0**：初始版本
  - 基础变基功能
  - 选择目标分支
  - 安全推送（--force-with-lease）
  - MR创建提示

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题，请联系开发团队或提交Issue。