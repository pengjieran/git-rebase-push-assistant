# 发布指南 / Publishing Guide

## 版本 1.0.0 正式发布准备清单

### 1. 构建插件

```bash
# 清理之前的构建
./gradlew clean

# 构建插件
./gradlew buildPlugin

# 输出位置
# build/distributions/git-plugin-1.0.0.zip
```

### 2. 验证插件

```bash
# 检查插件兼容性
./gradlew verifyPlugin

# 在沙盒环境测试
./gradlew runIde
```

**测试清单**：
- [ ] 变基与推送基本流程
- [ ] AI 提交消息生成（需配置有效的 OpenAI API）
- [ ] GitLab MR 自动创建（需配置 Token）
- [ ] Arthas 热修复脚本生成
- [ ] 中英文界面切换
- [ ] 配置持久化
- [ ] 错误处理和用户提示

### 3. 文档检查

- [x] `README.md` - 英文文档
- [x] `README_CN.md` - 中文文档
- [x] `CHANGELOG.md` - 更新日志
- [x] `RELEASE_NOTES.md` - 发布说明
- [x] `plugin.xml` - 插件描述
- [x] `gradle.properties` - 版本号更新为 `1.0.0`

### 4. 首次发布到 JetBrains Marketplace

#### 步骤：

1. **创建账号**
   - 访问 https://plugins.jetbrains.com/
   - 使用 JetBrains Account 登录

2. **上传插件**
   - 点击 "Upload Plugin"
   - 上传 `build/distributions/git-plugin-1.0.0.zip`
   - 填写插件信息（从 `plugin.xml` 复制）

3. **填写详细信息**
   - **Name**: Git Rebase and Push Assistant
   - **Category**: Version Control Systems
   - **Tags**: git, rebase, gitlab, ai, openai, arthas, hotfix
   - **License**: 根据项目实际情况选择（建议 Apache 2.0 或 MIT）
   - **Description**: 从 `plugin.xml` 中的 `<description>` 复制
   - **Change Notes**: 从 `CHANGELOG.md` 复制 1.0.0 版本内容

4. **添加截图和演示**
   - 主界面截图（变基对话框）
   - AI 生成提交消息截图
   - Arthas 脚本生成截图
   - 配置页面截图

5. **提交审核**
   - 首次提交需要人工审核（通常 1-3 个工作日）
   - 审核通过后插件将在 Marketplace 上线

### 5. 后续自动化发布（审核通过后）

#### 生成 Publish Token：
1. 访问 https://plugins.jetbrains.com/author/me/tokens
2. 创建新 Token（需要 `Marketplace` 权限）
3. 复制 Token

#### 设置环境变量：
```bash
export PUBLISH_TOKEN="perm:your-token-here"
```

#### 自动发布：
```bash
./gradlew publishPlugin
```

### 6. GitHub Release

#### 创建 Release：
```bash
# 创建并推送 tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

#### 在 GitHub 上：
1. 访问 Releases 页面
2. 点击 "Draft a new release"
3. 选择 tag `v1.0.0`
4. 标题: `v1.0.0 - 首次正式发布`
5. 描述: 从 `RELEASE_NOTES.md` 复制
6. 附件: 上传 `git-plugin-1.0.0.zip`
7. 发布

### 7. 发布后宣传

- [ ] 更新项目 README 添加 Marketplace 徽章
- [ ] 在相关技术社区发布（掘金、V2EX、Reddit）
- [ ] 公司内部推广（如适用）
- [ ] 收集用户反馈

### 8. 监控和维护

#### 收集指标：
- 下载量
- 评分和评论
- Issue 反馈
- 崩溃报告

#### 快速响应：
- 严重 Bug: 1-2 天内发布 hotfix 版本（1.0.1）
- 功能请求: 记录到 GitHub Issues，纳入下个版本规划
- 文档问题: 随时更新

---

## 版本号规则（语义化版本）

- **MAJOR (1.x.x)**: 不兼容的 API 变更
- **MINOR (x.1.x)**: 向后兼容的功能新增
- **PATCH (x.x.1)**: 向后兼容的问题修复

### 下一版本规划：

- **1.0.1** - Hotfix（如有紧急 Bug）
- **1.1.0** - 功能增强（如 GitHub PR 支持）
- **2.0.0** - 重大架构调整（如多仓库支持）

---

## 联系方式

- **Email**: support@examplecn.com
- **Issues**: https://github.com/your-org/git-plugin/issues
- **Marketplace**: https://plugins.jetbrains.com/plugin/XXXXX-git-rebase-and-push-assistant

---

**当前状态**: ✅ 准备就绪，可以发布 1.0.0 版本