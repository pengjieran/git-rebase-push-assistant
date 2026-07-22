# Arthas Hotfix Script Generator

## 功能概述

这是一个集成在 IntelliJ IDEA 插件中的工具，用于生成 Arthas Java 热修复脚本。它可以将编译后的 `.class` 文件转换为 Base64 编码，并生成一个可以直接在生产环境中执行的 shell 脚本。

## 使用场景

当您需要在生产环境中热更新 Java 类，而不想重启应用程序时，可以使用此功能：

1. 在本地修复了一个 bug 并编译生成了新的 `.class` 文件
2. 需要将修复快速部署到生产环境
3. 使用 Arthas 的 `retransform` 命令进行热加载

## 使用步骤

### 1. 触发功能

在 IntelliJ IDEA 中，有两种方式可以触发此功能：

- **菜单方式**：`Tools` → `Generate Arthas Hotfix Script`
- **右键菜单**：在项目视图中右键点击，选择 `Generate Arthas Hotfix Script`

### 2. 选择编译输出目录

对话框会自动检测以下常见的编译输出目录：

- `target/classes` (Maven)
- `build/classes/java/main` (Gradle Java)
- `build/classes/kotlin/main` (Gradle Kotlin)
- `out/production/classes` (IntelliJ IDEA)

如果自动检测失败，您可以手动选择包含 `.class` 文件的目录。

### 3. 扫描并选择 .class 文件

点击 **"扫描"** 按钮后，对话框会递归扫描所选目录下的所有 `.class` 文件，并在表格中显示：

- **Class Name**: 完整的类名（包名.类名）
- **Path**: 文件的绝对路径
- **Size**: 文件大小

从列表中选择需要热修复的 `.class` 文件。

### 4. 生成脚本

点击 **"OK"** 后，插件会：

1. 读取选中的 `.class` 文件
2. 将其编码为 Base64 格式
3. 生成包含完整说明的 shell 脚本
4. 显示生成的脚本内容

### 5. 保存或复制脚本

在脚本预览对话框中，您可以：

- **复制到剪贴板**：将脚本内容复制到剪贴板，方便直接粘贴
- **保存到文件**：将脚本保存为 `.sh` 文件（自动设置为可执行）
- **关闭**：关闭对话框

## 生成的脚本格式

生成的脚本包含以下内容：

```bash
#!/bin/bash
# Arthas Hotfix Script for <ClassName>
# Generated: <timestamp>

set -e

TEMP_DIR="/tmp"
CLASS_NAME="<ClassName>"
TIMESTAMP="<timestamp>"
TXT_FILE="${TEMP_DIR}/${CLASS_NAME}_${TIMESTAMP}.txt"
CLASS_FILE="${TEMP_DIR}/${CLASS_NAME}_${TIMESTAMP}.class"

echo "==> Arthas Hotfix Script for $CLASS_NAME"
echo "==> Decoding Base64 content to $TXT_FILE..."

# Write Base64 content to temporary text file
cat > "$TXT_FILE" << 'EOF_BASE64'
<base64-encoded-class-content>
EOF_BASE64

echo "==> Decoding to .class file at $CLASS_FILE..."
base64 -d < "$TXT_FILE" > "$CLASS_FILE"

echo "==> Class file created successfully"
echo ""
echo "==> Next steps:"
echo "    1. Attach Arthas to your Java process:"
echo "       java -jar arthas-boot.jar"
echo ""
echo "    2. Run the retransform command in Arthas:"
echo "       retransform $CLASS_FILE"
echo ""
echo "==> Retransform command (copy and paste into Arthas):"
echo "retransform $CLASS_FILE"
```

## 在生产环境中使用

### 1. 上传脚本到服务器

```bash
scp <ClassName>_hotfix_<timestamp>.sh user@server:/tmp/
```

### 2. 在服务器上执行脚本

```bash
ssh user@server
cd /tmp
chmod +x <ClassName>_hotfix_<timestamp>.sh
./<ClassName>_hotfix_<timestamp>.sh
```

脚本执行后，会在 `/tmp` 目录下生成解码后的 `.class` 文件。

### 3. 启动 Arthas 并附加到 Java 进程

```bash
java -jar arthas-boot.jar
```

选择需要热修复的 Java 进程。

### 4. 执行 retransform 命令

脚本会输出准备好的 `retransform` 命令，直接复制粘贴到 Arthas 中执行：

```bash
retransform /tmp/<ClassName>_<timestamp>.class
```

### 5. 验证修复结果

使用 Arthas 的其他命令验证类是否已成功重新加载：

```bash
# 查看类的加载信息
sc -d <ClassName>

# 反编译类查看修改
jad <ClassName>
```

### 6. 清理临时文件（可选）

修复成功后，可以清理临时文件：

```bash
rm -f /tmp/<ClassName>_<timestamp>.txt /tmp/<ClassName>_<timestamp>.class
```

## 技术实现

### 核心组件

1. **ArthasHotfixAction**: 主入口 Action，添加到 Tools 菜单和项目右键菜单
2. **ArthasHotfixDialog**: 文件选择对话框，支持目录扫描和 `.class` 文件列表展示
3. **ArthasScriptOutputDialog**: 脚本预览对话框，支持复制和保存功能
4. **ArthasHotfixService**: 核心服务，负责读取 `.class` 文件、Base64 编码和脚本生成

### Base64 编码

使用 Java 标准库的 `java.util.Base64` 进行编码：

```kotlin
val classBytes = classFile.readBytes()
val base64Content = Base64.getEncoder().encodeToString(classBytes)
```

### 脚本可执行权限

在 Unix-like 系统（Linux/macOS）上，保存的脚本会自动设置可执行权限：

```kotlin
if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
    file.setExecutable(true)
}
```

## 国际化支持

功能支持中英文双语：

- 中文（zh_CN）：默认显示中文界面和提示信息
- 英文（en）：英文界面和提示信息

## 注意事项

1. **类版本兼容性**：确保修复的 `.class` 文件与目标 JVM 版本兼容
2. **包名和类名一致**：修复的类必须与原类的包名和类名完全一致
3. **方法签名不能改变**：Arthas retransform 不支持修改方法签名（参数、返回值类型）
4. **静态字段初始化**：静态字段的初始化值不会重新执行
5. **备份原文件**：在生产环境操作前，建议先备份原 `.class` 文件
6. **测试环境验证**：建议先在测试环境验证热修复效果

## 限制

Arthas retransform 功能的限制：

- 不能增加/删除/重命名字段和方法
- 不能修改方法的参数类型、返回值类型
- 不能修改类的继承关系
- 不能增加/删除类的注解
- 不能修改已执行的静态初始化块

## 相关资源

- [Arthas 官方文档](https://arthas.aliyun.com/)
- [Arthas retransform 命令](https://arthas.aliyun.com/doc/retransform.html)
- [Java Instrumentation API](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)

## 许可证

本功能作为 Git Rebase and Push Assistant 插件的一部分，遵循相同的许可证。