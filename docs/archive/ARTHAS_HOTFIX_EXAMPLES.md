# Arthas 热修复功能使用示例

## 场景：修复生产环境的 NPE 问题

### 问题描述

生产环境中 `UserService.java` 的 `getUserInfo()` 方法出现 `NullPointerException`：

```java
package com.example.service;

public class UserService {
    public String getUserInfo(Long userId) {
        User user = userRepository.findById(userId);
        // Bug: user 可能为 null
        return user.getName() + " - " + user.getEmail();
    }
}
```

### 修复步骤

#### 1. 修复代码

```java
package com.example.service;

public class UserService {
    public String getUserInfo(Long userId) {
        User user = userRepository.findById(userId);
        // Fixed: 添加 null 检查
        if (user == null) {
            return "User not found";
        }
        return user.getName() + " - " + user.getEmail();
    }
}
```

#### 2. 本地编译

```bash
# Maven 项目
mvn clean compile

# Gradle 项目
./gradlew compileJava
```

#### 3. 使用插件生成热修复脚本

1. 在 IDEA 中打开项目
2. 点击菜单 `Tools` → `Generate Arthas Hotfix Script`
3. 自动检测到编译输出目录：`target/classes`
4. 点击 "扫描" 按钮
5. 在列表中找到并选择 `com.example.service.UserService`
6. 点击 "OK"
7. 在弹出的对话框中点击 "保存到文件"
8. 保存为 `UserService_hotfix_1721520000000.sh`

#### 4. 上传到生产服务器

```bash
scp UserService_hotfix_1721520000000.sh prod-server:/tmp/
```

#### 5. 在生产服务器上执行

```bash
# 登录服务器
ssh prod-server

# 执行脚本生成 class 文件
cd /tmp
chmod +x UserService_hotfix_1721520000000.sh
./UserService_hotfix_1721520000000.sh
```

输出示例：
```
==> Arthas Hotfix Script for UserService
==> Decoding Base64 content to /tmp/UserService_1721520000000.txt...
==> Decoding to .class file at /tmp/UserService_1721520000000.class...
==> Class file created successfully (Size: 2847 bytes)

==> Next steps:
    1. Attach Arthas to your Java process:
       java -jar arthas-boot.jar

    2. Run the retransform command in Arthas:
       retransform /tmp/UserService_1721520000000.class

==> Retransform command (copy and paste into Arthas):
retransform /tmp/UserService_1721520000000.class
```

#### 6. 启动 Arthas

```bash
# 下载 Arthas（如果尚未安装）
curl -O https://arthas.aliyun.com/arthas-boot.jar

# 启动并附加到 Java 进程
java -jar arthas-boot.jar
```

选择目标 Java 进程（例如进程 ID: 12345）：

```
[INFO] arthas-boot version: 3.7.1
[INFO] Found existing java process, please choose one and input the serial number of the process, eg : 1. Then hit ENTER.
* [1]: 12345 com.example.Application
  [2]: 12346 org.apache.catalina.startup.Bootstrap
1
```

#### 7. 执行热加载

复制脚本输出的 retransform 命令，粘贴到 Arthas 中：

```bash
[arthas@12345]$ retransform /tmp/UserService_1721520000000.class
retransform success, size: 1, classes:
com.example.service.UserService
```

#### 8. 验证修复

使用 Arthas 反编译查看修改后的代码：

```bash
[arthas@12345]$ jad com.example.service.UserService

ClassLoader:
+-sun.misc.Launcher$AppClassLoader@18b4aac2
  +-sun.misc.Launcher$ExtClassLoader@1e643faf

Location:
/app/lib/application.jar

public class UserService {
    public String getUserInfo(Long userId) {
        User user = this.userRepository.findById(userId);
        if (user == null) {
            return "User not found";
        }
        return user.getName() + " - " + user.getEmail();
    }
}

Affect(row-cnt:1) cost in 56 ms.
```

#### 9. 测试修复效果

```bash
# 使用 watch 命令观察方法调用
[arthas@12345]$ watch com.example.service.UserService getUserInfo "{params,returnObj}" -x 2

# 触发一个请求测试
# 当 userId 不存在时，应该返回 "User not found" 而不是抛出 NPE
```

#### 10. 清理临时文件

```bash
[arthas@12345]$ quit

# 退出 Arthas 后清理
rm -f /tmp/UserService_1721520000000.txt /tmp/UserService_1721520000000.class
```

## 常见场景

### 场景 1：修复日志打印错误

**问题**：某个方法的日志级别设置错误，导致大量无用日志

```java
// 修复前
logger.info("Processing item: " + item.getDetails()); // 太详细的日志

// 修复后
logger.debug("Processing item: " + item.getDetails()); // 改为 debug 级别
```

### 场景 2：调整业务逻辑阈值

**问题**：风控阈值设置过严，需要临时调整

```java
// 修复前
if (amount > 1000) {
    return false;
}

// 修复后
if (amount > 5000) { // 临时放宽限制
    return false;
}
```

### 场景 3：修复资源泄漏

**问题**：忘记关闭资源

```java
// 修复前
InputStream is = new FileInputStream(file);
processData(is);
// 忘记关闭

// 修复后
InputStream is = new FileInputStream(file);
try {
    processData(is);
} finally {
    if (is != null) is.close();
}
```

## 最佳实践

### 1. 版本管理

为每次热修复创建记录：

```bash
# 创建热修复记录目录
mkdir -p ~/hotfix-history/$(date +%Y%m%d)

# 保存脚本和说明
cp UserService_hotfix_*.sh ~/hotfix-history/$(date +%Y%m%d)/
echo "Fix NPE in getUserInfo() method" > ~/hotfix-history/$(date +%Y%m%d)/README.txt
```

### 2. 回滚准备

保存原始 class 文件：

```bash
# 在执行 retransform 前，先导出原始类
[arthas@12345]$ dump com.example.service.UserService

# 如果需要回滚
[arthas@12345]$ retransform /tmp/original/UserService.class
```

### 3. 监控验证

使用 Arthas 监控方法执行：

```bash
# 监控方法调用次数和耗时
[arthas@12345]$ monitor -c 5 com.example.service.UserService getUserInfo

# 跟踪方法调用堆栈
[arthas@12345]$ trace com.example.service.UserService getUserInfo
```

### 4. 告知团队

热修复完成后：

1. 在工单系统中记录修复操作
2. 通知相关开发和运维人员
3. 安排正式版本发布，替换热补丁
4. 测试环境验证

## 故障排查

### 问题：retransform 失败

```bash
[arthas@12345]$ retransform /tmp/UserService_1721520000000.class
retransform error! java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the schema
```

**原因**：修改了方法签名或字段

**解决**：检查修改内容，确保只修改了方法体内部的逻辑

### 问题：找不到类

```bash
[arthas@12345]$ retransform /tmp/UserService_1721520000000.class
retransform error! Can not find class: com.example.service.UserService
```

**原因**：类名或包名不匹配

**解决**：
1. 使用 `sc` 命令搜索类：`sc *UserService*`
2. 确保包名完全一致
3. 重新编译生成正确的 class 文件

### 问题：class 版本不兼容

```bash
retransform error! java.lang.UnsupportedClassVersionError
```

**原因**：编译的 class 文件版本高于运行环境的 JVM 版本

**解决**：在编译时指定目标版本
```bash
# Maven
mvn clean compile -Dmaven.compiler.target=1.8

# Gradle
./gradlew clean compileJava -PtargetCompatibility=1.8
```

## 总结

Arthas 热修复功能适用于：

✅ **适合的场景**：
- 紧急 bug 修复（NPE、资源泄漏等）
- 临时调整业务参数
- 日志级别调整
- 性能问题优化

❌ **不适合的场景**：
- 需要修改类结构（字段、方法签名）
- 需要增加新功能
- 涉及多个类的复杂修改
- 长期运行的修复（应该发布正式版本）

**安全提示**：热修复是一种临时解决方案，修复后应尽快发布正式版本。