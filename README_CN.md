# Git 增强插件

[![Version](https://img.shields.io/badge/version-1.0.3-orange)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![JetBrains](https://img.shields.io/badge/IntelliJ-2025.3.5-blue)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)]()

为 IntelliJ IDEA 提供强大的 Git 工作流增强功能，包括智能变基推送、AI 提交消息生成和 Arthas 热修复脚本生成。

## ✨ 核心特性

### 🔄 智能变基与推送
- **一键操作**：自动完成 fetch → rebase → push 全流程
- **安全推送**：使用 `--force-with-lease` 防止覆盖他人提交
- **自动提交**：变基前自动提交所有变更文件
- **分支建议**：智能识别并推荐 master/main/develop 分支
- **实时进度**：显示操作进度和友好的错误提示

### 🤖 AI 提交消息生成
- **智能分析**：基于文件变更和 git diff 自动生成提交消息
- **可配置**：支持自定义 OpenAI 兼容 API（Base URL、Model ID、API Key）
- **测试连接**：配置页面提供 API 连接测试功能
- **中文优化**：生成的提交消息遵循最佳实践

### 🔥 Arthas 热修复脚本
- **快速生成**：右键点击 `.class` 文件即可生成热修复脚本
- **Base64 编码**：自动编码类文件并生成完整的 shell 脚本
- **批量支持**：可同时选中多个 `.class` 文件批量处理
- **灵活输出**：支持复制到剪贴板或保存为可执行脚本

### 🚀 GitLab 集成
- **自动创建 MR**：推送后自动创建 Merge Request
- **安全存储**：使用系统密钥链存储 Personal Access Token
- **智能解析**：自动识别 GitLab URL 和项目路径（支持 SSH/HTTPS、子组）
- **友好回退**：API 失败时提供预填充的手动创建链接

## 📦 安装

### 方式一：从源码构建

```bash
# 克隆仓库
git clone <repository-url>
cd git-plugin

# 构建插件
./gradlew buildPlugin

# 生成的插件位于: build/distributions/git-plugin-*.zip
```

### 方式二：安装到 IDEA

1. 打开 `Preferences/Settings` → `Plugins`
2. 点击 ⚙️ → `Install Plugin from Disk...`
3. 选择构建生成的 ZIP 文件
4. 重启 IDEA

## 🚀 快速开始

### 1️⃣ 变基并推送

**使用场景**：将 feature 分支同步到最新的 master

1. 打开 Git 提交窗口（`Cmd+K` / `Ctrl+K`）
2. 点击 **"变基并推送"** 按钮（或通过菜单 `Git` → `变基并推送`）
3. 选择目标分支（如 `master`）
4. 可选勾选 **"推送后自动提交 merge 请求"**
5. 点击 **"变基并推送"**

插件会自动执行：
```bash
# 如果有未提交的变更
git add .
git commit -m "你的提交消息"

# 变基流程
git fetch origin master
git rebase origin/master
git push --force-with-lease origin feature/your-branch

# 如果勾选了自动创建 MR
# 调用 GitLab API 创建 Merge Request
```

### 2️⃣ AI 生成提交消息

**前置配置**：

1. 打开 `Preferences/Settings` → `Tools` → `Git Rebase & Push`
2. 配置 OpenAI API：
   - **Base URL**：API 端点（如 `https://api.openai.com/v1`）
   - **Model ID**：模型名称（如 `gpt-4` 或 `gpt-3.5-turbo`）
   - **API Key**：你的 API 密钥
3. 点击 **"测试连接"** 验证配置
4. 点击 **"应用"** 保存

**使用方法**：

1. 在变基对话框的提交消息区域
2. 点击 **"AI生成"** 按钮
3. 等待 AI 分析变更并生成提交消息
4. 根据需要修改生成的消息

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

### 3️⃣ 生成 Arthas 热修复脚本

**使用场景**：为已编译的 Java 类生成 Arthas 热修复脚本

1. 在项目视图中找到编译输出目录（如 `target/classes` 或 `build/classes`）
2. 选中一个或多个 `.class` 文件
3. 右键菜单选择 **"Generate Arthas Hotfix Script"**
4. 在弹出的对话框中选择：
   - **复制到剪贴板**：快速复制脚本内容
   - **保存到文件**：保存为可执行的 `.sh` 脚本
   - **关闭**：仅查看

**生成的脚本示例**：

```bash
#!/bin/bash
# Arthas Hotfix Script for UserService
# Generated: 2026-07-21T12:34:56Z

# 脚本会自动：
# 1. 解码 Base64 编码的类文件
# 2. 创建临时 .class 文件
# 3. 提供 Arthas retransform 命令

# 使用方法：
# 1. 上传脚本到目标服务器
# 2. chmod +x UserService_hotfix_*.sh
# 3. ./UserService_hotfix_*.sh
# 4. 附加 Arthas 并执行 retransform 命令
```

## ⚙️ 配置

### Git 变基设置

打开 `Preferences/Settings` → `Tools` → `Git Rebase & Push`

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| Default Target Branch | 默认目标分支 | `master` |
| Auto Stash | 自动暂存未提交的更改 | ✅ 启用 |
| Notify on Success | 成功后显示通知 | ✅ 启用 |

### OpenAI API 配置

| 配置项 | 说明 | 必填 |
|--------|------|------|
| Base URL | API 端点地址 | ✅ |
| Model ID | 使用的模型名称 | ✅ |
| API Key | API 密钥 | ✅ |

### GitLab Token 配置

首次创建 MR 时会提示输入 Personal Access Token：

1. 访问 GitLab：`用户设置` → `Access Tokens`
2. 创建新 Token，勾选 `api` 权限
3. 复制 Token 并粘贴到插件提示框中
4. Token 将安全存储在系统密钥链中

## 🏗️ 项目结构

```
git-plugin/
├── src/main/kotlin/com/examplecn/
│   ├── action/                    # UI 层
│   │   ├── GitRebaseAndPushAction.kt         # 主操作入口
│   │   ├── UnifiedRebaseDialog.kt            # 变基对话框
│   │   ├── ArthasHotfixAction.kt             # Arthas 脚本生成
│   │   └── ArthasScriptOutputDialog.kt       # 脚本输出对话框
│   ├── service/                   # 业务逻辑层
│   │   ├── GitRebaseService.kt               # Git 操作服务
│   │   ├── MergeRequestService.kt            # MR/PR 创建服务
│   │   ├── OpenAIService.kt                  # OpenAI API 服务
│   │   └── ArthasHotfixService.kt            # Arthas 脚本服务
│   ├── config/                    # 配置层
│   │   ├── GitRebaseSettings.kt              # 设置持久化
│   │   └── GitRebaseSettingsConfigurable.kt  # 设置 UI
│   └── bundle/                    # 国际化
│       └── GitRebaseBundle.kt                # 资源包访问
├── src/main/resources/
│   ├── META-INF/plugin.xml                   # 插件清单
│   └── messages/
│       ├── GitRebaseBundle.properties        # 英文资源
│       └── GitRebaseBundle_zh_CN.properties  # 中文资源
└── build.gradle.kts                          # 构建配置
```

## 🛠️ 技术栈

- **语言**：Kotlin 1.9+
- **平台**：IntelliJ Platform 2025.3.5
- **依赖**：Git4Idea（IDEA 内置）
- **构建工具**：Gradle 8.x
- **JDK**：17+

## 🧪 开发

### 运行测试 IDE

```bash
./gradlew runIde
```

启动包含插件的 IDEA 测试实例。

### 运行测试

```bash
./gradlew test
```

### 构建插件

```bash
./gradlew buildPlugin
```

输出位置：`build/distributions/git-plugin-*.zip`

### 验证插件兼容性

```bash
./gradlew verifyPlugin
```

## 📝 使用示例

### 场景 1：同步主分支最新代码

```bash
# 当前分支：feature/user-auth
# 目标：同步 master 最新代码

# 操作步骤：
1. Cmd+K 打开提交窗口
2. 点击 "变基并推送"
3. 选择目标分支：master
4. 点击 OK

# 结果：
✓ 成功变基到 master
✓ 推送到远程仓库
```

### 场景 2：提交 PR 并创建 MR

```bash
# 当前分支：feature/new-api
# 目标：变基到 develop 并创建 MR

# 操作步骤：
1. 确保代码已提交
2. 点击 "变基并推送"
3. 选择目标分支：develop
4. 勾选 "推送后自动提交 merge 请求"
5. 点击 OK

# 结果：
✓ 变基到 develop
✓ 推送成功
✓ 自动创建 MR：feature/new-api → develop
✓ 在通知中显示 MR 链接
```

### 场景 3：生产环境热修复

```bash
# 场景：生产环境发现 Bug，需要紧急热修复

# 操作步骤：
1. 本地修复代码并编译
2. 在 target/classes 中找到修改的 .class 文件
3. 右键 → "Generate Arthas Hotfix Script"
4. 保存为 hotfix.sh
5. 上传到生产服务器
6. 执行脚本并使用 Arthas retransform

# 结果：
✓ 无需重启服务即可修复 Bug
✓ 最小化服务中断时间
```

## ❓ 常见问题

### Q1：变基失败怎么办？

**A**：变基失败通常是因为有冲突。插件会显示错误信息，此时需要：

1. 手动解决冲突：`git status` 查看冲突文件
2. 编辑冲突文件并标记为已解决：`git add <file>`
3. 继续变基：`git rebase --continue`
4. 重新推送：`git push --force-with-lease`

### Q2：为什么 AI 生成失败？

**A**：检查以下几点：

- ✅ API Key 是否正确
- ✅ Base URL 是否可访问
- ✅ Model ID 是否存在
- ✅ 网络连接是否正常
- ✅ API 配额是否充足

使用 "测试连接" 按钮验证配置。

### Q3：GitLab Token 存储在哪里？

**A**：Token 使用 IntelliJ 的 `PasswordSafe` 存储，根据操作系统不同：

- **macOS**：Keychain Access
- **Windows**：Windows Credential Manager
- **Linux**：系统密钥环或加密文件

### Q4：支持 GitHub 吗？

**A**：目前仅支持 GitLab 自动创建 MR。GitHub PR 自动创建功能尚未实现，但会提供预填充的手动创建链接。

### Q5：Arthas 脚本如何使用？

**A**：生成的脚本包含详细的使用说明。基本流程：

```bash
# 1. 上传脚本到服务器
scp hotfix.sh user@server:/tmp/

# 2. 赋予执行权限
chmod +x /tmp/hotfix.sh

# 3. 执行脚本（会生成 .class 文件）
/tmp/hotfix.sh

# 4. 启动 Arthas 并附加到 Java 进程
java -jar arthas-boot.jar

# 5. 在 Arthas 中执行 retransform
retransform /tmp/YourClass_*.class

# 6. 验证修复是否生效
```

## 🐛 已知限制

- ❌ 多仓库项目仅操作第一个仓库
- ❌ GitHub PR 自动创建尚未实现
- ❌ 不支持交互式 rebase（如 `-i`）
- ❌ Arthas 脚本仅支持单个类文件（不支持内部类拆分）

## 🔮 路线图

- [ ] 支持 GitHub PR 自动创建
- [ ] 支持多仓库项目选择
- [ ] 添加交互式冲突解决向导
- [ ] 支持自定义 AI Prompt 模板
- [ ] Arthas 脚本支持多类文件合并
- [ ] 添加变基历史记录
- [ ] 支持 Gitee 等其他平台

## 📄 许可证

Apache License 2.0

## 🙏 鸣谢

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij)
- [Git4Idea](https://github.com/JetBrains/intellij-community/tree/master/plugins/git4idea)
- [Arthas](https://arthas.aliyun.com/)

---

**提示**：如需英文文档，请查看 [README.md](README.md)