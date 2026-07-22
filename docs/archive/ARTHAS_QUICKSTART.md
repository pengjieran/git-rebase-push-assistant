# Arthas 热修复功能 - 快速入门

## 5 分钟上手指南

### 前提条件

- ✅ 已安装 IntelliJ IDEA 2025.3.5+
- ✅ 已安装本插件（Git Rebase and Push Assistant）
- ✅ 项目已成功编译，存在 `.class` 文件

### 步骤 1: 打开功能

两种方式任选其一：

**方式 A - 通过菜单**
```
Tools → Generate Arthas Hotfix Script
```

**方式 B - 通过右键**
```
在项目视图中右键点击 → Generate Arthas Hotfix Script
```

### 步骤 2: 选择文件

1. 对话框会自动扫描并显示编译输出目录
2. 如果没有自动检测到，点击浏览按钮手动选择目录
3. 点击 **"扫描"** 按钮
4. 从列表中选择需要热修复的 `.class` 文件
5. 点击 **"OK"**

### 步骤 3: 保存脚本

在脚本预览对话框中：

- **复制到剪贴板** - 快速复制，适合临时使用
- **保存到文件** - 保存为 `.sh` 文件，建议方式
- **关闭** - 不保存退出

### 步骤 4: 上传并执行

```bash
# 上传到服务器
scp UserService_hotfix_*.sh user@server:/tmp/

# SSH 登录
ssh user@server

# 执行脚本
cd /tmp
chmod +x UserService_hotfix_*.sh
./UserService_hotfix_*.sh
```

### 步骤 5: 使用 Arthas 热加载

```bash
# 启动 Arthas
java -jar arthas-boot.jar

# 选择 Java 进程（例如输入 1）
1

# 执行 retransform（从脚本输出中复制）
retransform /tmp/UserService_<timestamp>.class
```

### 步骤 6: 验证

```bash
# 反编译查看修改
jad com.example.service.UserService

# 监控方法调用
watch com.example.service.UserService methodName "{params,returnObj}" -x 2
```

## 常见问题

### Q1: 找不到 .class 文件？

**A**: 确保项目已编译：
```bash
# Maven
mvn clean compile

# Gradle
./gradlew compileJava

# IDEA
Build → Build Project (Ctrl+F9 / ⌘F9)
```

### Q2: 自动检测不到输出目录？

**A**: 手动选择以下目录之一：
- Maven: `target/classes`
- Gradle Java: `build/classes/java/main`
- Gradle Kotlin: `build/classes/kotlin/main`
- IntelliJ: `out/production/classes`

### Q3: retransform 失败？

**A**: 检查以下事项：
1. 类名和包名是否完全匹配
2. 是否修改了方法签名（不支持）
3. JVM 版本是否兼容
4. 类是否已经被加载

### Q4: 如何回滚热修复？

**A**: 使用 Arthas 导出原始类并重新加载：
```bash
# 先导出原始类
dump com.example.service.UserService

# 如需回滚
retransform /path/to/original/UserService.class
```

## 实际案例

### 案例 1: 修复 NPE

**问题代码**:
```java
public String getUserName(Long id) {
    User user = findUser(id);
    return user.getName(); // NPE if user is null
}
```

**修复代码**:
```java
public String getUserName(Long id) {
    User user = findUser(id);
    return user != null ? user.getName() : "Unknown";
}
```

**操作**: 编译 → 生成脚本 → 上传 → retransform

### 案例 2: 调整超时时间

**问题代码**:
```java
private static final int TIMEOUT = 3000; // 太短

public void connect() {
    socket.connect(host, port, TIMEOUT);
}
```

**修复代码**:
```java
private static final int TIMEOUT = 10000; // 增加到 10 秒

public void connect() {
    socket.connect(host, port, TIMEOUT);
}
```

**操作**: 编译 → 生成脚本 → 上传 → retransform

### 案例 3: 修复日志级别

**问题代码**:
```java
logger.info("Processing user: " + user.getDetails()); // 太详细
```

**修复代码**:
```java
logger.debug("Processing user: " + user.getDetails()); // 改为 debug
```

**操作**: 编译 → 生成脚本 → 上传 → retransform

## 最佳实践

### ✅ 推荐做法

1. **先在测试环境验证**
   ```bash
   # 在测试环境先执行一遍
   ./hotfix_script.sh
   # 验证无误后再在生产环境执行
   ```

2. **保存脚本副本**
   ```bash
   # 建立热修复记录
   mkdir -p ~/hotfix-records/$(date +%Y%m%d)
   cp *.sh ~/hotfix-records/$(date +%Y%m%d)/
   ```

3. **导出原始类**
   ```bash
   # 在 retransform 前先导出
   [arthas@12345]$ dump com.example.service.UserService
   ```

4. **监控修复效果**
   ```bash
   # 持续观察方法调用
   [arthas@12345]$ monitor -c 10 com.example.service.UserService methodName
   ```

### ❌ 避免做法

1. ❌ 不要在生产环境直接测试未验证的热修复
2. ❌ 不要修改方法签名或类结构
3. ❌ 不要长期依赖热修复（应发布正式版本）
4. ❌ 不要忘记清理临时文件

## 快捷键（待实现）

```
Ctrl+Alt+A (Windows/Linux)
⌘⌥A (macOS)
```

## 技术支持

遇到问题？

1. 查看 [完整功能文档](ARTHAS_HOTFIX_FEATURE.md)
2. 查看 [使用示例](ARTHAS_HOTFIX_EXAMPLES.md)
3. 查看 [实现总结](ARTHAS_IMPLEMENTATION_SUMMARY.md)
4. 提交 Issue 到项目仓库

## 相关链接

- [Arthas 官方文档](https://arthas.aliyun.com/)
- [Arthas GitHub](https://github.com/alibaba/arthas)
- [插件项目主页](https://github.com/pengjieran/git-plugin)

---

**快速命令参考**

```bash
# 1. 编译项目
mvn clean compile

# 2. 生成脚本（在 IDEA 中操作）
Tools → Generate Arthas Hotfix Script

# 3. 上传脚本
scp hotfix.sh server:/tmp/

# 4. 执行脚本
ssh server
cd /tmp && chmod +x hotfix.sh && ./hotfix.sh

# 5. 启动 Arthas
java -jar arthas-boot.jar

# 6. 热加载
retransform /tmp/ClassName_timestamp.class

# 7. 验证
jad com.example.ClassName
```

**预计耗时**: 5-10 分钟完成整个流程