# UI界面中文化

## 修改内容

已将所有面向用户的界面文本从英文改为中文，确保用户体验一致性。

### 修改文件清单

#### 1. GitRebaseAndPushAction.kt

**错误提示信息：**
- "No Git repository found" → "未找到Git仓库"
- "No current branch" → "未找到当前分支"
- "Error" → "错误"

**进度提示信息：**
- "Rebasing $currentBranch onto $targetBranch" → "正在将 $currentBranch 变基到 $targetBranch"
- "Checking for uncommitted changes..." → "检查未提交的变更..."
- "Waiting for commit selection..." → "等待选择提交文件..."
- "Fetching remote branch $targetBranch..." → "正在拉取远程分支 $targetBranch..."
- "Rebasing $currentBranch onto $targetBranch..." → "正在将 $currentBranch 变基到 $targetBranch..."
- "Committing selected files..." → "正在提交选中的文件..."
- "Pushing to remote..." → "正在推送到远程仓库..."

**成功/完成信息：**
- "Successfully rebased $currentBranch onto $targetBranch and pushed to remote." → "已成功将 $currentBranch 变基到 $targetBranch 并推送到远程仓库"
- "Rebase Complete" → "变基完成"
- "Rebase failed: ${e.message}" → "变基失败: ${e.message}"

**Merge请求相关：**
- "Rebase, commit and push completed.\n\nMerge request created:\n${result.url}" → "变基、提交和推送已完成\n\nMerge请求已创建:\n${result.url}"
- "Merge Request Not Created" → "Merge请求未创建"
- "Merge Request Failed" → "Merge请求失败"
- "Settings > Tools > Git Rebase Plugin" → "设置 > 工具 > Git变基插件"

**配置对话框：**
- "Please select a target branch" → "请选择一个目标分支"
- "Cannot rebase onto the current branch" → "不能变基到当前分支"
- "Changes will be force-pushed using --force-with-lease." → "变更将使用 --force-with-lease 强制推送"

#### 2. CommitFilesDialog.kt

**标签文本：**
- "Commit信息:" → "提交信息:"

**错误提示：**
- "请填写commit信息" → "请填写提交信息"

#### 3. MergeRequestService.kt

**自动生成的Merge请求标题和描述：**
- "Merge $sourceBranch into $targetBranch" → "合并 $sourceBranch 到 $targetBranch"
- "Auto-generated merge request from IntelliJ IDEA Git Plugin" → "由IntelliJ IDEA Git插件自动创建的合并请求"

### 编译验证

```bash
./gradlew build
BUILD SUCCESSFUL
```

所有修改已通过编译验证。

## 中文化覆盖范围

✅ 错误对话框标题和内容  
✅ 进度提示信息  
✅ 成功/完成提示  
✅ 配置对话框文本  
✅ 表单标签  
✅ API生成的内容（MR标题和描述）  

## 注意事项

- 保留了技术术语的常用形式（如"Git"、"Merge请求"、"--force-with-lease"）
- 错误标题统一使用"错误"而非"Error"
- 进度提示使用"正在..."格式，符合中文表达习惯
- 所有用户可见的界面文本均已中文化