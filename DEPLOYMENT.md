# 部署指南

## 📦 构建插件

### 1. 环境要求

- **JDK**: 17或更高版本
- **Gradle**: 8.x (项目自带wrapper)
- **Git**: 2.0+ (用于测试)

验证环境：
```bash
java -version
./gradlew --version
git --version
```

### 2. 克隆项目

```bash
git clone <repository-url>
cd git-plugin
```

### 3. 构建插件包

```bash
# 清理并构建
./gradlew clean buildPlugin

# 查看构建结果
ls -lh build/distributions/
```

输出：
```
git-plugin-1.0.0-SNAPSHOT.zip  (~21KB)
```

## 🚀 安装到IDEA

### 方式1: 通过磁盘安装 (推荐)

1. 打开IntelliJ IDEA
2. `File` → `Settings` (Windows/Linux) 或 `IntelliJ IDEA` → `Preferences` (macOS)
3. 导航到 `Plugins`
4. 点击右上角齿轮图标 ⚙️
5. 选择 `Install Plugin from Disk...`
6. 浏览并选择 `build/distributions/git-plugin-1.0.0-SNAPSHOT.zip`
7. 点击 `OK`
8. 重启 IDEA

### 方式2: 手动安装

```bash
# 解压到IDEA插件目录
# macOS
unzip build/distributions/git-plugin-1.0.0-SNAPSHOT.zip -d ~/Library/Application\ Support/JetBrains/IntelliJIdea2025.3/plugins/

# Linux
unzip build/distributions/git-plugin-1.0.0-SNAPSHOT.zip -d ~/.local/share/JetBrains/IntelliJIdea2025.3/plugins/

# Windows
unzip build/distributions/git-plugin-1.0.0-SNAPSHOT.zip -d %USERPROFILE%\AppData\Roaming\JetBrains\IntelliJIdea2025.3\plugins\
```

重启IDEA。

## ✅ 验证安装

### 1. 检查插件是否加载

1. `Settings` → `Plugins`
2. 在已安装列表中搜索 "Git-plugin" 或 "Git Rebase"
3. 确认状态为 ✅ Enabled

### 2. 测试功能

1. 打开任意Git项目
2. 按 `Cmd+K` (macOS) 或 `Ctrl+K` (Windows/Linux) 打开提交窗口
3. 查看是否有 "Rebase and Push" 按钮
如果看到按钮，恭喜安装成功！🎉

### 3. 测试基本功能

**前置条件**：
- 项目是Git仓库
- 有远程仓库 (origin)
- 当前在某个开发分支上

**测试步骤**：
1. 点击 "Rebase and Push" 按钮
2. 应该看到分支选择对话框
3. 下拉框中应该显示远程分支列表
4. 取消对话框

如果以上步骤都正常，插件工作正常！

## 🔧 开发模式安装

### 运行测试IDE

```bash
./gradlew runIde
```

这会启动一个包含插件的IDEA测试实例，无需手动安装。

优点：
- 快速测试
- 独立沙盒环境
- 便于调试

## 📤 发布到JetBrains Marketplace

### 1. 准备工作

更新版本信息：
```gradle
// gradle.properties
version = 1.0.0
```

更新 CHANGELOG.md：
```markdown
## [1.0.0] - 2024-XX-XX
### Added
- Git变基和推送功能
- 分支选择对话框
- 安全推送选项
- MR创建提示
```

### 2. 构建发布版本

```bash
./gradlew clean buildPlugin
```

### 3. 发布到Marketplace

**方式A: 使用Gradle任务**
```bash
export PUBLISH_TOKEN=<your-token>
./gradlew publishPlugin
```

**方式B: 手动上传**
1. 访问 https://plugins.jetbrains.com/plugin/upload
2. 登录JetBrains账户
3. 上传 ZIP 文件
4. 填写插件信息
5. 提交审核

## 🐛 故障排除

### 问题1: 构建失败

**症状**: `./gradlew buildPlugin` 失败

**解决**:
```bash
# 清理缓存
./gradlew clean --refresh-dependencies

# 确认JDK版本
java -version  # 应该是17+

# 查看详细错误
./gradlew buildPlugin --stacktrace
```

### 问题2: IDEA无法识别插件

**症状**: 安装后找不到插件

**检查**:
1. ZIP文件是否正确生成？
2. IDEA版本是否兼容？(需要2025.3.5+)
3. 是否重启了IDEA？

**解决**:
```bash
# 查看插件内容
unzip -l build/distributions/git-plugin-*.zip

# 应该包含:
# - META-INF/plugin.xml
# - lib/*.jar
```

### 问题3: 插件加载但按钮不显示

**原因**: 可能不在Git项目中

**检查**:
```bash
cd your-project
git status  # 应该显示Git信息
git remote -v  # 应该有remote
```

### 问题4: 依赖问题

**症状**: 运行时找不到Git4Idea

**解决**: 确认 plugin.xml 中有：
```xml
<depends>Git4Idea</depends>
```

## 📋 部署清单

在部署前检查：

- [ ] 代码已提交到Git
- [ ] 版本号已更新
- [ ] CHANGELOG已更新
- [ ] 所有测试通过
- [ ] 构建成功
- [ ] 在测试IDE中验证功能
- [ ] 文档已更新
- [ ] README准确

## 🔄 更新现有安装

### 用户端更新

如果已经安装旧版本：

1. `Settings` → `Plugins`
2. 找到 "Git-plugin"
3. 点击 Update（如果可用）

或者手动：
1. 卸载旧版本
2. 重启IDEA
3. 安装新版本
4. 重启IDEA

### 开发者更新

```bash
# 拉取最新代码
git pull origin main

# 重新构建
./gradlew clean buildPlugin

# 重新安装
# (使用上述安装方法)
```

## 📊 发布统计

安装后可以在JetBrains Marketplace查看：
- 下载量
- 用户评分
- 兼容性报告
- 崩溃报告

## 🆘 获取帮助

- 📧 提交Issue: <repository-url>/issues
- 📚 查看文档: README.md, USAGE_GUIDE.md
- 💬 讨论区: <if available>

## 🎉 部署完成

恭喜！您已成功部署Git变基推送插件。

下一步：
- 📖 阅读 [QUICK_START.md](QUICK_START.md) 了解使用方法
- 🧪 在测试项目中试用功能
- 📢 分享给团队成员
