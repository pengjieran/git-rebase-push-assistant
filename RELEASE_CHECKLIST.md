# 发布检查清单 v1.0.0

## ✅ 已完成项目

### 1. 版本更新
- [x] `gradle.properties` 版本号更新为 `1.0.0`
- [x] `CHANGELOG.md` 日期更新为 2026-07-22
- [x] `RELEASE_NOTES.md` 日期更新为 2026-07-22
- [x] `plugin.xml` 描述更新为富文本格式

### 2. 构建验证
- [x] 执行 `./gradlew clean` 清理旧构建
- [x] 执行 `./gradlew buildPlugin` 构建成功
- [x] 生成插件包: `build/distributions/git-rebase-push-assistant-1.0.0.zip` (104 KB)

### 3. 文档完善
- [x] 创建 `PUBLISHING.md` - 发布指南
- [x] 创建 `RELEASE_CHECKLIST.md` - 本检查清单
- [x] `README.md` - 英文文档完整
- [x] `README_CN.md` - 中文文档完整
- [x] `CHANGELOG.md` - 版本历史记录
- [x] `RELEASE_NOTES.md` - 发布说明

## 📋 待执行项目

### 4. 功能测试（手动）
在沙盒环境中测试以下功能：

```bash
./gradlew runIde
```

- [ ] **变基与推送基本流程**
  - [ ] 创建测试分支
  - [ ] 修改文件并执行变基操作
  - [ ] 验证 fetch → rebase → push 流程
  - [ ] 验证 `--force-with-lease` 推送

- [ ] **AI 提交消息生成**
  - [ ] 配置 OpenAI API（Settings → Tools → Git Rebase & Push）
  - [ ] 点击 "AI生成" 按钮
  - [ ] 验证生成的提交消息格式
  - [ ] 测试连接验证功能

- [ ] **GitLab MR 自动创建**
  - [ ] 勾选 "推送后自动提交merge请求"
  - [ ] 输入 GitLab Personal Access Token
  - [ ] 验证 MR 自动创建成功
  - [ ] 检查 Token 是否安全存储

- [ ] **Arthas 热修复脚本**
  - [ ] 选中 `.class` 文件
  - [ ] 右键 → "Generate Arthas Hotfix Script"
  - [ ] 验证生成的 Base64 脚本
  - [ ] 测试复制到剪贴板功能

- [ ] **国际化测试**
  - [ ] 切换到英文界面
  - [ ] 切换到中文界面
  - [ ] 验证所有文本正常显示

- [ ] **配置持久化**
  - [ ] 修改默认分支设置
  - [ ] 重启 IDE
  - [ ] 验证配置保留

- [ ] **错误处理**
  - [ ] 测试无网络情况下的 AI 生成
  - [ ] 测试无效的 GitLab Token
  - [ ] 测试 rebase 冲突处理
  - [ ] 验证错误提示友好性

### 5. Git 操作
- [ ] 提交所有变更
  ```bash
  git add .
  git commit -m "Release version 1.0.0"
  ```

- [ ] 创建版本标签
  ```bash
  git tag -a v1.0.0 -m "Release version 1.0.0 - 首次正式发布"
  git push origin v1.0.0
  ```

- [ ] 推送到远程仓库
  ```bash
  git push origin feature/support-auto-rebase-and-push
  ```

- [ ] 创建 Pull Request/Merge Request 到 main 分支

### 6. JetBrains Marketplace 发布

#### 首次提交步骤：
1. [ ] 访问 https://plugins.jetbrains.com/
2. [ ] 点击 "Upload Plugin"
3. [ ] 上传 `build/distributions/git-rebase-push-assistant-1.0.0.zip`
4. [ ] 填写插件基本信息：
   - **Name**: Git Rebase and Push Assistant
   - **Category**: Version Control Systems
   - **Tags**: git, rebase, gitlab, ai, openai, arthas, hotfix, automation
   - **License**: Apache 2.0 / MIT（根据实际选择）

5. [ ] 复制 `plugin.xml` 中的 `<description>` 内容
6. [ ] 复制 `CHANGELOG.md` 中的 1.0.0 版本内容到 Change Notes

#### 准备截图（建议尺寸 1280x800）：
- [ ] 主对话框截图（变基操作界面）
- [ ] AI 生成提交消息截图
- [ ] 设置页面截图
- [ ] Arthas 脚本生成截图
- [ ] GitLab MR 创建成功截图

7. [ ] 提交审核（预计 1-3 个工作日）

### 7. GitHub Release
- [ ] 访问项目 GitHub Releases 页面
- [ ] 点击 "Draft a new release"
- [ ] 选择 tag `v1.0.0`
- [ ] 标题: `v1.0.0 - 首次正式发布`
- [ ] 描述: 复制 `RELEASE_NOTES.md` 内容
- [ ] 附件: 上传 `git-rebase-push-assistant-1.0.0.zip`
- [ ] 点击 "Publish release"

### 8. 文档更新
- [ ] 在 `README.md` 顶部添加徽章：
  ```markdown
  [![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
  [![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
  [![Rating](https://img.shields.io/jetbrains/plugin/r/rating/PLUGIN_ID)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
  ```
  （PLUGIN_ID 需要在 Marketplace 审核通过后替换）

### 9. 宣传推广（可选）
- [ ] 技术社区发布
  - [ ] 掘金
  - [ ] CSDN
  - [ ] 知乎
  - [ ] V2EX
  - [ ] Reddit (r/IntelliJIDEA)

- [ ] 公司内部
  - [ ] 内部技术分享
  - [ ] 团队群组通知
  - [ ] 文档中心更新

### 10. 监控和维护
- [ ] 设置 JetBrains Marketplace 邮件通知
- [ ] 监控下载量和评分
- [ ] 跟踪用户评论和反馈
- [ ] 创建 GitHub Issues 模板
- [ ] 准备快速响应流程（严重 Bug 1-2 天内发布 hotfix）

## 📊 发布统计（待填写）

### Marketplace 指标
- 首日下载量: ___
- 首周下载量: ___
- 用户评分: ___/5
- 评论数: ___

### GitHub 指标
- Stars: ___
- Forks: ___
- Issues: ___

## 🐛 已知问题记录

### 当前限制
1. 多仓库项目仅操作第一个仓库
2. GitHub PR 自动创建尚未实现（返回手动创建链接）
3. 不支持交互式 rebase (`-i`)

### 计划改进
- v1.1.0: 实现 GitHub PR 自动创建
- v1.1.0: 支持多仓库选择
- v1.2.0: 添加交互式 rebase 支持
- v1.2.0: 支持自定义提交消息模板

## ✅ 最终确认

- [ ] 所有测试通过
- [ ] 文档完整准确
- [ ] 插件包构建成功
- [ ] Git 标签已创建
- [ ] 准备好提交到 Marketplace

---

**发布负责人**: ___________  
**发布日期**: 2026-07-22  
**版本号**: 1.0.0  
**状态**: 🟡 准备中 → 🟢 已发布