# Co-Authored-By 功能说明

## 功能概述

在 Git 变基推送对话框的提交信息区域，当选择"Co-Authored-By"追加类型时，会弹出一个专门的选择对话框，支持：

1. **预定义 AI 助手列表**：可从 10 个常见 AI 助手中选择
2. **多选支持**：可以同时选择多个作者
3. **自定义输入**：对于列表中没有的，支持手动输入名称和邮箱
4. **格式化输出**：自动生成标准格式的 `Co-Authored-By: Name <email>`
5. **多作者分隔**：多个作者用分号（`;`）分隔

## 预定义 AI 助手列表

- Claude Code <claude-code@anthropic.com>
- Cursor <cursor@cursor.sh>
- Yonwork <Yonwork@yonyou.com>
- OPENAI <openai@openai.com>
- GitHub Copilot <copilot@github.com>
- CodeBuddy <codebuddy@ai-assistant.com>
- Lingma <lingma@alibaba.com>
- Trae <trae@trae.ai>
- DeepSeek <deepseek@deepseek.ai>
- GLM <glm@glm.com>

## 使用方法

1. 打开 Git 变基推送对话框（在 Git 提交界面点击"变基并提交推送"按钮）
2. 在提交信息区域下方，找到"自动追加"工具栏
3. 从下拉框中选择"Co-Authored-By"
4. 点击"追加"按钮，弹出选择对话框
5. 在对话框中：
   - **从预定义列表选择**：在左侧列表中选中一个或多个 AI 助手，点击"添加选中 →"按钮
   - **自定义输入**：在底部输入框中填写名称和邮箱，点击"添加自定义"按钮
   - **移除已选择**：在右侧"已选择"列表中选中要移除的项，点击"移除选中"按钮
6. 确认选择后，点击"OK"
7. 选中的作者会以 `Co-Authored-By: Name <email>;Co-Authored-By: Name2 <email2>` 格式追加到提交信息末尾

## 输出格式示例

### 单个作者
```
feat: 添加新功能;Co-Authored-By: Claude Code <claude-code@anthropic.com>
```

### 多个作者
```
feat: 添加新功能;Co-Authored-By: Claude Code <claude-code@anthropic.com>;Co-Authored-By: Cursor <cursor@cursor.sh>;Co-Authored-By: GitHub Copilot <copilot@github.com>
```

## 技术实现

### 新增文件
- `CoAuthoredByDialog.kt`：Co-Authored-By 选择对话框

### 修改文件
- `UnifiedRebaseDialog.kt`：
  - 修改了 `appendContent()` 方法，当选择 Co-Authored-By 时弹出选择对话框
  - 修改了 `requiresInput()` 方法，Co-Authored-By 不再需要输入框（改用对话框）

### 关键特性
- 使用 `Set<AuthorInfo>` 存储选中的作者，通过 email 去重
- 左侧预定义列表支持多选（`MULTIPLE_INTERVAL_SELECTION`）
- 右侧已选择列表实时显示格式化后的结果
- 自定义输入时会验证名称和邮箱不能为空
- 确认时至少需要选择一个作者

## 注意事项

1. 作者去重基于邮箱地址，相同邮箱只会添加一次
2. 生成的 Co-Authored-By 行会自动追加到提交信息末尾
3. 多个作者之间用分号分隔，符合 Git 提交消息约定
4. 对话框关闭前必须至少选择一个作者，否则会提示错误
