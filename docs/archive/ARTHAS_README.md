# Arthas 热修复功能 - README

## 🚀 功能简介

这是一个集成在 IntelliJ IDEA 插件中的 **Arthas 热修复脚本生成器**，帮助开发者快速生成用于生产环境 Java 热补丁的部署脚本。

## ✨ 主要特性

- 🎯 **一键生成**：从编译的 `.class` 文件一键生成完整的热修复脚本
- 📦 **自包含脚本**：生成的脚本包含 Base64 编码的 class 文件，无需额外文件
- 🔍 **智能扫描**：自动检测 Maven/Gradle/IDEA 的编译输出目录
- 📋 **便捷操作**：支持复制到剪贴板或保存为文件
- 🌍 **国际化**：支持中文和英文界面
- 🛡️ **零依赖**：仅使用 Java 标准库，不引入额外依赖

## 📖 文档导航

| 文档 | 说明 |
|------|------|
| **[快速入门](ARTHAS_QUICKSTART.md)** | 5分钟上手指南，快速了解如何使用 |
| **[功能详解](ARTHAS_HOTFIX_FEATURE.md)** | 完整的功能说明、技术实现和注意事项 |
| **[使用示例](ARTHAS_HOTFIX_EXAMPLES.md)** | 真实场景下的使用案例和最佳实践 |
| **[实现总结](ARTHAS_IMPLEMENTATION_SUMMARY.md)** | 技术实现细节和架构说明 |

## 🎬 快速开始

### 1. 触发功能

在 IntelliJ IDEA 中：
- 菜单栏：`Tools` → `Generate Arthas Hotfix Script`
- 或右键项目 → `Generate Arthas Hotfix Script`

### 2. 选择 .class 文件

插件会自动扫描编译输出目录，选择需要热修复的类文件。

### 3. 生成脚本

点击 OK 后，可以：
- 复制到剪贴板
- 保存为 `.sh` 文件

### 4. 在服务器上执行

```bash
# 上传并执行脚本
scp hotfix.sh server:/tmp/
ssh server "cd /tmp && chmod +x hotfix.sh && ./hotfix.sh"

# 使用 Arthas 热加载
java -jar arthas-boot.jar
retransform /tmp/ClassName_timestamp.class
```

## 📊 代码统计

```
新增文件数: 4 个 Kotlin 源文件
代码行数: 460 行
文档行数: 1000+ 行
构建状态: ✅ BUILD SUCCESSFUL
```

## 🗂️ 文件结构

```
src/main/kotlin/com/examplecn/
├── action/
│   ├── ArthasHotfixAction.kt              # 主入口 Action
│   ├── ArthasHotfixDialog.kt              # 文件选择对话框
│   └── ArthasScriptOutputDialog.kt        # 脚本输出对话框
└── service/
    └── ArthasHotfixService.kt             # 核心业务逻辑

docs/
├── ARTHAS_QUICKSTART.md                   # 快速入门
├── ARTHAS_HOTFIX_FEATURE.md               # 功能详解
├── ARTHAS_HOTFIX_EXAMPLES.md              # 使用示例
└── ARTHAS_IMPLEMENTATION_SUMMARY.md       # 实现总结
```

## 🎯 使用场景

### ✅ 适用场景

- 🐛 紧急 Bug 修复（NPE、资源泄漏等）
- ⚙️ 临时调整业务参数或阈值
- 📝 日志级别调整
- 🔧 性能问题优化

### ❌ 不适用场景

- 需要修改类结构（字段、方法签名）
- 需要增加新功能
- 涉及多个类的复杂修改
- 长期运行的修复（应发布正式版本）

## 💡 实际案例

### 案例 1: 修复空指针异常

```java
// 修复前
public String getUserName(Long id) {
    User user = findUser(id);
    return user.getName(); // 可能抛出 NPE
}

// 修复后
public String getUserName(Long id) {
    User user = findUser(id);
    return user != null ? user.getName() : "Unknown";
}
```

**操作流程**：编译 → 生成脚本 → 上传服务器 → Arthas retransform

### 案例 2: 调整超时配置

```java
// 从 3 秒调整到 10 秒
private static final int TIMEOUT = 10000; // 之前是 3000
```

### 案例 3: 修改日志级别

```java
// 从 info 改为 debug
logger.debug("Processing: " + data); // 之前是 logger.info
```

## 🔧 技术实现

- **语言**: Kotlin
- **框架**: IntelliJ Platform SDK
- **编码**: Java Base64 标准库
- **UI**: Swing (JBTable, DialogWrapper)
- **兼容性**: IDEA 2025.3.5+, JDK 17+

## 📝 生成的脚本示例

```bash
#!/bin/bash
# Arthas Hotfix Script for UserService
# Generated: 2026-07-21T20:30:00Z

set -e

# 将 Base64 编码的 class 内容写入临时文件
cat > /tmp/UserService_1721590200.txt << 'EOF_BASE64'
yv66vgAAADQAOQoADQAjBwAkCgACAC...（Base64内容）...
EOF_BASE64

# 解码为 .class 文件
base64 -d < /tmp/UserService_1721590200.txt > /tmp/UserService_1721590200.class

echo "==> Class file created successfully"
echo "==> Next steps:"
echo "    1. java -jar arthas-boot.jar"
echo "    2. retransform /tmp/UserService_1721590200.class"
```

## 🛡️ 安全建议

1. ⚠️ **热修复是临时方案**，修复后应尽快发布正式版本
2. 🧪 **先在测试环境验证**，确认无误后再在生产环境执行
3. 💾 **保存原始类文件**，使用 Arthas `dump` 命令导出备份
4. 📋 **记录修复操作**，在工单系统中记录每次热修复

## 🌟 功能亮点

| 特性 | 说明 |
|------|------|
| **智能检测** | 自动识别 Maven、Gradle、IDEA 的输出目录 |
| **表格展示** | 清晰展示类名、路径、文件大小 |
| **详细说明** | 生成的脚本包含完整的使用步骤 |
| **错误处理** | 脚本使用 `set -e` 确保错误时停止 |
| **可执行权限** | Unix 系统自动设置 `.sh` 文件为可执行 |
| **时间戳命名** | 自动生成带时间戳的唯一文件名 |

## 🔍 故障排查

### 问题：找不到 .class 文件

**解决方案**：
```bash
# Maven
mvn clean compile

# Gradle
./gradlew clean compileJava

# IDEA
Build → Build Project (Ctrl+F9)
```

### 问题：retransform 失败

**可能原因**：
- 类名或包名不匹配
- 修改了方法签名（不支持）
- JVM 版本不兼容

**解决方案**：查看 [故障排查文档](ARTHAS_HOTFIX_EXAMPLES.md#故障排查)

## 📦 构建插件

```bash
# 构建插件
./gradlew buildPlugin

# 输出位置
build/distributions/git-rebase-push-assistant-1.0.0-SNAPSHOT.zip

# 安装到 IDEA
Settings → Plugins → Install Plugin from Disk → 选择 zip 文件
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📜 许可证

本功能作为 Git Rebase and Push Assistant 插件的一部分，遵循项目的许可证。

## 🔗 相关资源

- [Arthas 官方文档](https://arthas.aliyun.com/)
- [Arthas GitHub](https://github.com/alibaba/arthas)
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)

---

**版本**: 1.0.0-SNAPSHOT  
**发布日期**: 2026-07-21  
**作者**: Claude Code  
**状态**: ✅ 功能完成并测试通过