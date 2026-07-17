# 项目结构说明

## 完整目录树

```
git-plugin/
├── src/
│   ├── main/
│   │   ├── kotlin/com/examplecn/
│   │   │   ├── action/
│   │   │   │   └── GitRebaseAndPushAction.kt      # 主Action和UI对话框
│   │   │   ├── config/
│   │   │   │   └── GitRebaseSettings.kt           # 配置管理（未来扩展）
│   │   │   ├── service/
│   │   │   │   └── GitRebaseService.kt            # Git操作核心服务
│   │   │   ├── MyToolWindowFactory.kt             # 示例ToolWindow
│   │   │   └── MyMessageBundle.kt                 # 国际化消息
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml                     # 插件配置文件
│   │       └── messages/
│   │           └── MyMessageBundle.properties     # 消息资源
│   └── test/
│       └── kotlin/com/examplecn/
│           └── service/
│               └── GitRebaseServiceTest.kt        # 单元测试
├── build.gradle.kts                                # Gradle构建配置
├── settings.gradle.kts                             # Gradle设置
├── gradle.properties                               # Gradle属性
├── README_REBASE.md                                # 功能说明文档
├── USAGE_GUIDE.md                                  # 使用指南
└── PROJECT_STRUCTURE.md                            # 本文件
```

## 核心文件说明

### 1. GitRebaseAndPushAction.kt

**职责**：
- Action入口点
- UI对话框展示
- 用户交互处理
- 后台任务协调

**主要类**：
- `GitRebaseAndPushAction`: 主Action类
  - `update()`: 控制按钮可见性
  - `actionPerformed()`: 处理点击事件
  - `executeRebaseAndPushInBackground()`: 后台任务执行

- `RebaseConfigDialog`: 配置对话框
  - 分支选择下拉框
  - MR创建选项复选框
  - 输入验证

**关键代码**：
```kotlin
class GitRebaseAndPushAction : AnAction("Rebase and Push") {
    override fun actionPerformed(e: AnActionEvent) {
        // 1. 获取项目和仓库
        // 2. 显示配置对话框
        // 3. 执行后台任务
    }
}
```

### 2. GitRebaseService.kt

**职责**：
- Git命令封装
- 错误处理
- 业务逻辑实现
**主要方法**：
```kotlin
@Service(Service.Level.PROJECT)
class GitRebaseService(private val project: Project) {
    fun fetchRemoteBranch()      // 拉取远程分支
    fun rebaseOnto()             // 执行变基
    fun forcePushBranch()        // 强制推送
    fun getRemoteBranches()      // 获取分支列表
    fun executeRebaseWorkflow()  // 完整流程
}
```

### 3. plugin.xml

**关键配置**：
```xml
<depends>Git4Idea</depends>

<actions>
  <action id="GitRebaseAndPush"
          class="com.examplecn.action.GitRebaseAndPushAction">
    <add-to-group group-id="Vcs.CommitExecutor.Actions" anchor="last"/>
  </action>
</actions>
```

## 技术架构

### 分层设计

```
┌─────────────────────────────────┐
│     UI Layer (Action)           │
├─────────────────────────────────┤
│   Service Layer (Service)       │
├─────────────────────────────────┤
│   Git4Idea API                  │
└─────────────────────────────────┘
```

## 未来扩展

### 1. GitLab API集成

创建 `GitLabMRService.kt`：
```kotlin
@Service(Service.Level.PROJECT)
class GitLabMRService(private val project: Project) {
    fun createMergeRequest(
        sourceBranch: String,
        targetBranch: String,
        title: String
    ): String // 返回MR URL
}
```

### 2. 冲突智能处理

添加冲突检测和提示：
```kotlin
fun detectConflicts(repository: GitRepository): List<ConflictFile>
fun suggestResolution(conflict: ConflictFile): String
```

### 3. 批量变基

支持多分支同时变基：
```kotlin
fun batchRebase(
    repository: GitRepository,
    branches: List<String>,
    targetBranch: String
)
```

## 开发指南

### 添加新Action

1. 在 `action/` 包下创建新Action类
2. 在 `plugin.xml` 中注册
3. 实现 `AnAction` 接口

### 添加新Service

1. 在 `service/` 包下创建Service类
2. 使用 `@Service` 注解
3. 通过 `project.service<YourService>()` 获取实例

### 添加配置项

1. 在 `GitRebaseSettings.State` 中添加字段
2. 创建配置UI（可选）
3. 在Service中读取配置
