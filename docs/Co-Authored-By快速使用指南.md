# Co-Authored-By 快速使用指南

## 5 步快速上手

### 步骤 1：打开变基推送对话框
在 Git 提交界面，点击 **"变基并提交推送"** 按钮。

### 步骤 2：定位到追加工具栏
在对话框的 **"提交信息"** 区域下方，找到 **"自动追加"** 工具栏。

### 步骤 3：选择 Co-Authored-By 类型
从下拉框中选择 **"Co-Authored-By"**，然后点击 **"追加"** 按钮。

### 步骤 4：选择作者
在弹出的对话框中：

**方式 A：从预定义列表选择**
- 在左侧列表中选择一个或多个 AI 助手（按住 Ctrl/Cmd 多选）
- 点击 **"添加选中 →"** 按钮

**方式 B：自定义输入**
- 在底部输入框中填写名称和邮箱
- 点击 **"添加自定义"** 按钮

**移除不需要的**
- 在右侧列表中选择要移除的项
- 点击 **"移除选中"** 按钮

### 步骤 5：确认并提交
点击 **"OK"** 按钮，选中的作者会自动追加到提交信息末尾。

---

## 预定义 AI 助手列表

| AI 助手 | 邮箱 |
|---------|------|
| Claude Code | claude-code@anthropic.com |
| Cursor | cursor@cursor.sh |
| Yonwork | Yonwork@yonyou.com |
| OPENAI | openai@openai.com |
| GitHub Copilot | copilot@github.com |
| CodeBuddy | codebuddy@ai-assistant.com |
| Lingma | lingma@alibaba.com |
| Trae | trae@trae.ai |
| DeepSeek | deepseek@deepseek.ai |
| GLM | glm@glm.com |

---

## 示例

### 示例 1：单个 AI 助手
**操作**：选择 "Claude Code"

**结果**：
```
feat: 添加用户登录功能;Co-Authored-By: Claude Code <claude-code@anthropic.com>
```

### 示例 2：多个 AI 助手协作
**操作**：选择 "Claude Code"、"Cursor"、"GitHub Copilot"

**结果**：
```
feat: 实现复杂的数据处理逻辑;Co-Authored-By: Claude Code <claude-code@anthropic.com>;Co-Authored-By: Cursor <cursor@cursor.sh>;Co-Authored-By: GitHub Copilot <copilot@github.com>
```

### 示例 3：自定义作者
**操作**：
- 名称：Team Lead
- 邮箱：lead@company.com

**结果**：
```
refactor: 重构核心模块;Co-Authored-By: Team Lead <lead@company.com>
```

---

## 常见问题

### Q1：为什么我无法添加重复的作者？
**A**：系统会自动基于邮箱地址去重，相同邮箱只能添加一次，确保提交信息的整洁性。

### Q2：可以同时选择多个预定义作者吗？
**A**：可以！在左侧列表中按住 Ctrl（Windows/Linux）或 Cmd（Mac）键，点击多个作者，然后一次性添加。

### Q3：自定义输入的名称和邮箱有什么要求？
**A**：两者都不能为空。邮箱格式无强制验证，但建议使用标准邮箱格式。

### Q4：如何修改已添加的作者？
**A**：在右侧"已选择"列表中选择要移除的作者，点击"移除选中"按钮，然后重新添加正确的作者。

### Q5：多个作者之间用什么分隔？
**A**：系统自动使用分号（`;`）分隔多个 Co-Authored-By 行。

---

## 提示和技巧

### 💡 技巧 1：快速批量添加
如果经常需要添加相同的几个 AI 助手，可以：
1. 在左侧列表中按住 Ctrl/Cmd 选择它们
2. 一次性点击"添加选中 →"

### 💡 技巧 2：查看完整格式
添加作者后，在右侧"已选择"列表中可以实时预览最终追加到提交信息的完整格式。

### 💡 技巧 3：自定义常用作者
对于团队成员或其他常用作者，使用"自定义输入"功能，输入一次后可以直接在提交信息中看到效果。

---

## 需要帮助？

如果遇到问题，请查看：
- [完整功能说明](./Co-Authored-By功能说明.md)
- [测试清单](./Co-Authored-By测试清单.md)
- [实现总结](./实现总结.md)

或提交 Issue 到项目仓库。
