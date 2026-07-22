# 发布说明

## v1.0.0 - 首次正式发布 (2026-07-22)

### 🎉 核心功能

#### 1. 智能变基与推送
一键完成 Git 工作流，自动处理 fetch、rebase 和 push 操作。

**特性**：
- ✅ 自动提交变更文件
- ✅ 安全推送（`--force-with-lease`）
- ✅ 智能分支建议
- ✅ 实时进度显示

#### 2. AI 提交消息生成
集成 OpenAI API，智能生成规范的提交消息。

**特性**：
- ✅ 基于文件变更和 git diff 自动生成
- ✅ 支持自定义 API（OpenAI/Azure/本地模型）
- ✅ 一键测试连接
- ✅ 中文提交消息优化

**配置示例**：
```properties
# OpenAI 官方
Base URL: https://api.openai.com/v1
Model ID: gpt-4
API Key: sk-...

# Azure OpenAI
Base URL: https://your-resource.openai.azure.com/openai/deployments/your-deployment
Model ID: gpt-4
API Key: your-azure-key

# 本地模型（Ollama）
Base URL: http://localhost:11434/v1
Model ID: qwen:7b
API Key: (留空)
```

#### 3. Arthas 热修复脚本
为编译后的 Java 类文件生成 Arthas 热修复脚本。

**特性**：
- ✅ Base64 编码类文件
- ✅ 生成完整的 shell 脚本
- ✅ 支持批量处理
- ✅ 复制到剪贴板或保存为文件

**使用方法**：
1. 在项目视图选中 `.class` 文件
2. 右键 → "Generate Arthas Hotfix Script"
3. 选择复制或保存

#### 4. GitLab 集成
自动创建 Merge Request。

**特性**：
- ✅ 推送后自动创建 MR
- ✅ Token 安全存储（系统密钥链）
- ✅ 支持 SSH/HTTPS 远程仓库
- ✅ 支持 GitLab 子组

### 📋 系统要求

- **IDE**: IntelliJ IDEA 2025.3.5+
- **JDK**: 17+
- **操作系统**: macOS / Windows / Linux

### 📥 安装

```bash
# 构建插件
./gradlew buildPlugin

# 生成位置
build/distributions/git-plugin-1.0.0.zip
```

在 IDEA 中：
1. `Preferences/Settings` → `Plugins`
2. ⚙️ → `Install Plugin from Disk...`
3. 选择 ZIP 文件并重启

### 🔧 配置

#### OpenAI API
`Settings` → `Tools` → `Git Rebase & Push`

配置 Base URL、Model ID 和 API Key，然后点击"测试连接"验证。

#### GitLab Token
首次创建 MR 时会提示输入 Personal Access Token（需要 `api` 权限）。
Token 将安全存储在系统密钥链中。

### 🐛 已知问题

- ⚠️ 多仓库项目仅操作第一个仓库
- ⚠️ GitHub PR 自动创建尚未实现
- ⚠️ 不支持交互式 rebase（`-i`）

### 📚 文档

- [English README](README.md)
- [中文文档](README_CN.md)
- [更新日志](CHANGELOG.md)

### 🙏 致谢

感谢所有贡献者和开源项目：
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij)
- [Git4Idea](https://github.com/JetBrains/intellij-community/tree/master/plugins/git4idea)
- [Arthas](https://arthas.aliyun.com/)

---

**注意**: 详细的变更记录请查看 [CHANGELOG.md](CHANGELOG.md)