# Arthas 热修复脚本生成功能简化

## 变更概述

简化了 Arthas 热修复脚本生成功能的用户交互流程，移除了中间的文件选择对话框，直接使用 IDE 中用户选中的 `.class` 文件。

## 主要改动

### 1. **ArthasHotfixAction.kt** - 简化操作流程

**之前的流程：**
1. 用户触发操作
2. 显示 `ArthasHotfixDialog` 让用户选择目录
3. 扫描目录查找 `.class` 文件
4. 用户从列表中选择文件
5. 生成脚本并显示输出对话框

**现在的流程：**
1. 用户在 IDE 项目视图中选中 `.class` 文件
2. 右键菜单中触发操作（仅当选中了 `.class` 文件时才显示）
3. 直接生成脚本并显示输出对话框（保留复制到剪贴板/保存到文件的选择）

**关键变更：**
- 使用 `CommonDataKeys.VIRTUAL_FILE_ARRAY` 获取用户在 IDE 中选中的文件
- 在 `update()` 方法中检查选中的文件是否包含 `.class` 文件，动态控制操作的可见性和可用性
- 支持同时处理多个 `.class` 文件（批量生成）
- 移除了对 `ArthasHotfixDialog` 的依赖

### 2. **资源文件更新**

#### GitRebaseBundle.properties
添加新的错误提示：
```properties
arthas.error.no.class.selected=Please select at least one .class file in the project view
```

#### GitRebaseBundle_zh_CN.properties
添加中文翻译：
```properties
arthas.error.no.class.selected=请在项目视图中选择至少一个.class文件
```

## 用户体验改进

### 优势：
1. **更直观**：直接在项目视图中右键点击 `.class` 文件即可生成脚本
2. **减少步骤**：从 5 步减少到 3 步
3. **支持批量**：可以一次选中多个 `.class` 文件批量生成脚本
4. **上下文感知**：只有在选中 `.class` 文件时才显示操作，避免误操作
5. **保留灵活性**：最后一步仍然允许用户选择复制到剪贴板或保存到文件

### 使用方式：
1. 在 IDEA 的项目视图（Project）中找到编译输出目录（如 `target/classes` 或 `build/classes`）
2. 选中一个或多个 `.class` 文件
3. 右键菜单选择 "Generate Arthas Hotfix Script"（生成 Arthas 热修复脚本）
4. 在弹出的对话框中选择：
   - **复制到剪贴板**：快速复制脚本内容
   - **保存到文件**：保存为可执行的 `.sh` 脚本文件
   - **关闭**：查看后关闭对话框

## 技术细节

### 文件可见性控制
```kotlin
override fun update(e: AnActionEvent) {
    val project = e.project
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    
    // 只有选中了 .class 文件时才显示此操作
    val hasClassFile = selectedFiles?.any { it.extension == "class" && !it.isDirectory } == true
    
    e.presentation.isEnabledAndVisible = project != null && hasClassFile
}
```

### 批量处理
```kotlin
classFiles.forEach { virtualFile ->
    val classFile = File(virtualFile.path)
    generateHotfixScript(project, classFile)
}
```

## 未来可能的增强

1. 支持选中整个目录批量生成
2. 在生成前预览将要处理的文件列表
3. 生成多个脚本时提供批量保存选项
4. 添加快捷键支持

## 兼容性

- ✅ 保持了现有的 `ArthasScriptOutputDialog` 不变
- ✅ 保持了 `ArthasHotfixService` 的脚本生成逻辑不变
- ✅ 向后兼容，不影响现有功能
- ⚠️ `ArthasHotfixDialog.kt` 不再被使用（可以考虑删除）

## 构建状态

✅ 构建成功 - 所有更改已编译通过