# 功能测试清单

## 1. 基础Rebase功能测试

### 测试场景1: 标准变基流程
- [ ] 创建测试分支 `feature/test-1`
- [ ] 在Git提交窗口点击"变基并提交推送"按钮
- [ ] 选择目标分支 `main` 或 `master`
- [ ] **不勾选** "推送后自动提交merge请求"
- [ ] 点击OK
- [ ] 验证：
  - [ ] 能看到进度提示（Fetching → Rebasing → Pushing）
  - [ ] 操作成功完成
  - [ ] 显示成功消息
  - [ ] 远程分支已更新

### 测试场景2: 有未提交变更的变基
- [ ] 修改文件但不提交
- [ ] 点击"变基并提交推送"
- [ ] 验证：
  - [ ] 弹出文件选择对话框
  - [ ] 显示未提交的文件列表
  - [ ] 勾选要提交的文件
  - [ ] 输入commit信息
  - [ ] 验证commit和push成功

### 测试场景3: 分支建议
- [ ] 当前在feature分支
- [ ] 打开变基对话框
- [ ] 验证：
  - [ ] 下拉框预选了 `master` 或 `main` 或 `develop`
  - [ ] 当前分支不在列表中
  - [ ] 可以看到所有远程分支

## 2. GitLab MR自动创建测试

### 前置条件
- [ ] 项目remote为GitLab仓库
- [ ] 拥有有效的GitLab账号
- [ ] 能访问GitLab (网络正常)

### 测试场景4: 首次配置Token
- [ ] 创建测试分支 `feature/mr-test-1`
- [ ] 点击"变基并提交推送"
- [ ] 选择目标分支
- [ ] **勾选** "推送后自动提交merge请求"
- [ ] 点击OK
- [ ] 验证Token配置对话框：
  - [ ] 显示"请输入GitLab Personal Access Token"
  - [ ] 提供Token创建链接
  - [ ] 说明Token将安全存储
- [ ] 输入有效的GitLab Token（需有api权限）
- [ ] 验证：
  - [ ] Rebase和Push成功
  - [ ] 显示"MR创建成功"消息
  - [ ] 消息中包含MR的URL
  - [ ] 点击URL能打开浏览器访问MR

### 测试场景5: 使用已保存的Token
- [ ] 创建新测试分支 `feature/mr-test-2`
- [ ] 点击"变基并提交推送"
- [ ] **勾选** "推送后自动提交merge请求"
- [ ] 点击OK
- [ ] 验证：
  - [ ] **不再提示** 输入Token
  - [ ] 直接使用之前保存的Token
  - [ ] MR创建成功
  - [ ] 在GitLab上能看到新创建的MR

### 测试场景6: Token无效
- [ ] 在系统密钥链中删除保存的Token，或使用无效Token
- [ ] 创建测试分支 `feature/mr-test-3`
- [ ] 执行变基并创建MR
- [ ] 验证：
  - [ ] 显示API错误信息（如401 Unauthorized）
  - [ ] 提供手动创建MR的链接
  - [ ] 链接已预填充源分支和目标分支
  - [ ] 点击链接能打开GitLab MR创建页面

### 测试场景7: 网络问题
- [ ] 断开网络或使用无法访问的GitLab地址
- [ ] 尝试创建MR
- [ ] 验证：
  - [ ] 显示清晰的错误信息
  - [ ] 提供手动创建链接作为fallback

## 3. URL解析测试

### 测试场景8: SSH格式
验证以下remote URL格式能正确解析：
- [ ] `git@gitlab.com:username/project.git`
- [ ] `git@gitlab.com:group/project.git`  
- [ ] `git@gitlab.com:group/subgroup/project.git`
- [ ] `git@gitlab.example.com:group/project.git` (自托管)

### 测试场景9: HTTPS格式
- [ ] `https://gitlab.com/username/project.git`
- [ ] `https://gitlab.com/group/project.git`
- [ ] `https://gitlab.example.com/group/project.git`

### 测试场景10: GitHub仓库
- [ ] 使用GitHub仓库
- [ ] 勾选创建MR
- [ ] 验证：
  - [ ] 提示"GitHub PR自动创建功能暂未实现"
  - [ ] 提供GitHub PR创建链接
  - [ ] 链接格式: `https://github.com/user/repo/compare/target...source`

## 4. 边界情况测试

### 测试场景11: 没有origin remote
- [ ] 删除origin remote: `git remote remove origin`
- [ ] 尝试变基
- [ ] 验证：显示"未找到origin远程仓库"错误

### 测试场景12: 无法识别的平台
- [ ] 使用非GitLab/GitHub的remote（如Bitbucket）
- [ ] 尝试创建MR
- [ ] 验证：提示"无法识别远程仓库平台"

### 测试场景13: 取消Token输入
- [ ] 首次使用时在Token对话框点击Cancel
- [ ] 验证：
  - [ ] 不创建MR
  - [ ] 提供手动创建链接

### 测试场景14: Rebase冲突
- [ ] 创建会产生冲突的分支
- [ ] 尝试变基
- [ ] 验证：
  - [ ] 显示冲突错误
  - [ ] 提示需要手动解决
  - [ ] 不继续执行后续步骤

## 5. 安全性测试

### 测试场景15: Token存储
- [ ] 配置Token后检查系统密钥链
- [ ] macOS: 打开Keychain Access，搜索"GitLabToken"
- [ ] Windows: 打开Credential Manager
- [ ] Linux: 使用 `secret-tool lookup service GitLabToken`
- [ ] 验证：Token已安全存储

### 测试场景16: Token不会泄露
- [ ] 检查日志文件
- [ ] 检查错误消息
- [ ] 验证：Token从未以明文形式出现

## 6. 性能测试

### 测试场景17: 大型仓库
- [ ] 在有大量分支的仓库中测试
- [ ] 验证：分支列表加载速度可接受

### 测试场景18: API超时
- [ ] 模拟慢网络环境
- [ ] 验证：10秒超时后显示错误

## GitLab MR创建验证

创建成功后，在GitLab上检查MR是否包含：
- [ ] 正确的源分支（source_branch）
- [ ] 正确的目标分支（target_branch）
- [ ] 标题格式: "Merge {source} into {target}"
- [ ] 描述: "Auto-generated merge request from IntelliJ IDEA Git Plugin"

## 测试环境

### 必需工具
- IntelliJ IDEA 2025.3.5+
- JDK 17+
- Git 2.0+
- GitLab账号（用于MR测试）

### 测试数据
准备以下测试仓库：
1. GitLab SSH仓库
2. GitLab HTTPS仓库
3. GitHub仓库（用于验证fallback）
4. 自托管GitLab仓库（可选）

## 回归测试

每次修改后运行以下核心场景：
- [ ] 场景1: 基础变基
- [ ] 场景4: 首次配置Token
- [ ] 场景5: 使用已保存Token
- [ ] 场景6: Token无效处理