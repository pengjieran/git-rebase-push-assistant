# Git Rebase and Push Plugin

IntelliJ IDEA插件，在Git提交页面添加"Rebase and Push"功能按钮。

## 功能特性

- ✅ 选择目标分支进行变基
- ✅ 自动拉取目标分支最新内容
- ✅ 将当前开发分支变基到目标分支
- ✅ 自动推送到远程分支（使用 `--force-with-lease` 安全推送）
- ✅ 可选择是否创建Merge Request到目标分支
- ✅ 后台任务执行，带进度提示
- ✅ 智能分支建议（优先推荐 master/main/develop）

## 使用方法

### 1. 在Git提交页面使用

1. 打开IDEA的Git提交窗口（Commit窗口）
2. 在提交按钮旁边找到"Rebase and Push"按钮
3. 点击按钮，会弹出配置对话框

### 2. 配置变基选项

在对话框中可以：
- **选择目标分支**：从远程分支列表中选择要变基到的目标分支
- **创建MR选项**：勾选"Create Merge Request after push"可以在推送后提示创建MR

### 3. 执行流程

点击OK后，插件会自动执行以下操作：

1. **Fetch**: 拉取目标分支的最新内容
2. **Rebase**: 将当前分支变基到目标分支
3. **Push**: 使用 `--force-with-lease` 强制推送到远程
4. **提示创建MR**（如果选中）: 显示创建MR的信息

## 安全特性

- 使用 `--force-with-lease` 而不是 `--force`，防止覆盖他人推送的内容
- 在变基前会检查当前分支状态
- 不允许变基到当前分支
- 所有Git操作都有错误处理和提示

## 开发说明

### 项目结构

```
src/main/kotlin/com/examplecn/
├── action/
│   └── GitRebaseAndPushAction.kt    # 主Action和对话框
└── service/
    └── GitRebaseService.kt          # Git操作服务
```

### 构建插件

```bash
./gradlew buildPlugin
```

生成的插件文件位于：`build/distributions/`

### 运行测试

```bash
./gradlew runIde
```

这会启动一个带有插件的IDEA实例进行测试。

### 依赖

- IntelliJ IDEA 2025.3.5+
- Git4Idea 插件（IDEA内置）
- Kotlin

## 注意事项

1. **变基前确保工作区干净**：建议在变基前提交或暂存所有更改
2. **强制推送的风险**：虽然使用了 `--force-with-lease`，但仍需谨慎使用
3. **MR创建**：目前MR创建仅显示提示信息，需要在GitLab/GitHub手动创建
4. **未来增强**：可以集成GitLab/GitHub API实现自动创建MR

## 扩展功能（待实现）

- [ ] 集成GitLab API自动创建Merge Request
- [ ] 集成GitHub API自动创建Pull Request
- [ ] 支持Gitee等其他Git平台
- [ ] 变基冲突智能提示和处理
- [ ] 批量变基多个分支
- [ ] 自定义Git命令参数

## 许可证

项目许可证根据您的需要设置。