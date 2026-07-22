# GitLab MR 自动创建功能使用说明

## 功能概述

插件现已支持通过GitLab API自动创建Merge Request，无需手动跳转到GitLab网页。

## 使用步骤

### 1. 首次使用 - 配置GitLab Token

第一次使用自动创建MR功能时，插件会提示输入GitLab Personal Access Token：

1. 点击对话框中提供的链接，或手动访问：
   ```
   https://your-gitlab.com/-/user_settings/personal_access_tokens
   ```

2. 创建新Token，需要以下权限：
   - ✅ **api** - 完整API访问权限（必需）

3. 复制生成的Token

4. 在插件弹出的对话框中粘贴Token

5. Token会被安全存储在系统密钥链中（macOS Keychain / Windows Credential Manager / Linux Secret Service）

### 2. 正常使用流程

1. 在Git提交窗口点击 **"变基并提交推送"** 按钮

2. 选择目标分支

3. ✅ 勾选 **"推送后自动提交merge请求"**

4. 点击OK

5. 插件会自动：
   - Fetch目标分支
   - Rebase当前分支
   - Push到远程
   - **调用GitLab API创建MR**
   - 显示MR链接

## Token管理

### Token存储位置

Token存储在系统安全存储中：
- **macOS**: Keychain
- **Windows**: Credential Manager  
- **Linux**: Secret Service (如 gnome-keyring)

### 重新配置Token

如果需要更换Token（如Token过期或被撤销）：
1. 删除当前存储的Token（从系统密钥链中删除"GitLabToken"条目）
2. 下次使用时插件会重新提示输入

或者使用命令行：
```bash
# macOS
security delete-generic-password -s "GitLabToken"

# Linux (gnome-keyring)
secret-tool clear service GitLabToken
```

## API调用详情

插件会调用以下GitLab API：

```
POST https://your-gitlab.com/api/v4/projects/{project_path}/merge_requests

Headers:
  PRIVATE-TOKEN: your-token
  Content-Type: application/json

Body:
{
  "source_branch": "feature-branch",
  "target_branch": "main",
  "title": "Merge feature-branch into main",
  "description": "Auto-generated merge request from IntelliJ IDEA Git Plugin"
}
```

## 支持的GitLab格式

插件自动识别以下Git remote URL格式：

- SSH: `git@gitlab.com:group/project.git`
- SSH (子组): `git@gitlab.com:group/subgroup/project.git`
- HTTPS: `https://gitlab.com/group/project.git`
- 自托管GitLab: `git@gitlab.example.com:group/project.git`

## 错误处理

如果GitLab API调用失败，插件会：
1. 显示具体的错误信息
2. 提供手动创建MR的链接（预填充了源分支和目标分支）
3. 允许用户通过浏览器完成MR创建

## GitHub支持

目前GitHub PR自动创建功能暂未实现，使用GitHub仓库时插件会提供手动创建链接。

## 故障排查

### Token无效
**症状**: "401 Unauthorized"或"403 Forbidden"

**解决**:
- 检查Token是否有api权限
- 检查Token是否已过期
- 重新生成Token并配置

### 网络问题
**症状**: "Connection timeout"或"Unknown error"

**解决**:
- 检查网络连接
- 如果使用自托管GitLab，确保IDEA可以访问该地址
- 检查是否需要配置代理

### 项目路径解析失败
**症状**: "无法解析GitLab URL"

**解决**:
- 检查git remote URL格式是否正确
- 确认remote名称为"origin"
- 手动查看: `git remote -v`