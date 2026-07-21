# OpenAI 集成功能说明

## 功能概述

插件现已集成 OpenAI API，可以根据代码变更内容自动生成提交信息。

## 新增功能

### 1. OpenAI 配置页面

在 IntelliJ IDEA 设置中添加了配置页面：
- 路径：`Settings/Preferences` → `Tools` → `Git Rebase & Push`
- 可配置项：
  - **Base URL**: OpenAI API 基础地址（默认：`https://api.openai.com/v1`）
  - **模型ID**: 使用的模型（默认：`gpt-4o-mini`，也支持 `gpt-4`, `gpt-3.5-turbo` 等）
  - **API Key**: OpenAI API 密钥
  - **测试连接**: 点击测试按钮验证配置是否正确

### 2. AI 生成提交信息

在"变基并推送"对话框的提交信息区域新增了"AI生成"按钮：
- 点击按钮后，插件会自动：
  1. 获取当前所有变更文件列表
  2. 获取完整的 git diff 内容
  3. 调用 OpenAI API 生成提交信息
  4. 自动填充到提交信息文本框

### 3. 智能提示

- 如果 OpenAI 未配置，点击"AI生成"时会提示并引导用户前往配置页面
- 生成过程中按钮会显示"生成中..."状态
- 生成成功后会显示通知

## 使用步骤

### 第一步：配置 OpenAI

1. 打开 IntelliJ IDEA 设置
2. 导航到 `Tools` → `Git Rebase & Push`
3. 填写以下信息：
   ```
   Base URL: https://api.openai.com/v1
   模型ID: gpt-4o-mini
   API Key: sk-xxxxxxxxxxxxx
   ```
4. 点击"测试连接"按钮验证配置
5. 点击"OK"保存配置

### 第二步：使用 AI 生成提交信息

1. 修改代码后，打开 Git 提交界面
2. 点击"变基并提交推送"按钮
3. 选择目标分支
4. 在提交信息区域，点击"AI生成"按钮（带灯泡图标）
5. 等待几秒，提交信息将自动生成
6. 可以根据需要修改生成的提交信息
7. 点击"变基并推送"完成操作

## 技术实现

### 架构设计

1. **GitRebaseSettings**: 扩展配置类，新增 OpenAI 相关配置字段
2. **OpenAIService**: 新增服务类，负责调用 OpenAI API
   - `generateCommitMessage()`: 生成提交信息
   - `testConnection()`: 测试连接
3. **GitRebaseSettingsConfigurable**: 新增配置界面
4. **UnifiedRebaseDialog**: 扩展对话框，新增"AI生成"按钮
5. **GitRebaseService**: 扩展服务，新增 `getDiff()` 方法获取变更内容

### API 调用流程

```
用户点击"AI生成"
    ↓
获取变更文件列表 + git diff
    ↓
构建 prompt（中文提交信息生成提示）
    ↓
调用 OpenAI Chat Completions API
    ↓
解析响应并填充到文本框
```

### Prompt 设计

插件使用精心设计的 prompt 来生成高质量的中文提交信息：
- 要求简洁明了，一行为主，不超过50个字符
- 使用动词开头（如：修复、增加、优化、重构）
- 概括主要目的，不包含代码细节
- 直接返回提交信息内容，无需前缀或解释

### 安全性

- API Key 存储在 `gitRebasePlugin.xml` 配置文件中
- 配置界面使用密码框（`JBPasswordField`）遮蔽 API Key 显示
- 建议：后续可以集成 IntelliJ 的 `PasswordSafe` 服务，将密钥存储到系统密钥链

## 支持的 OpenAI 兼容服务

除了官方 OpenAI API，还支持兼容的第三方服务：
- Azure OpenAI Service
- 自托管的 OpenAI 兼容服务
- 国内的各种 LLM 服务（如：通义千问、文心一言等，需要兼容 OpenAI API 格式）

只需修改 Base URL 即可。

## 依赖说明

实现采用纯 JDK HttpURLConnection，无需引入外部 HTTP 客户端库：
- HTTP 请求：`java.net.HttpURLConnection`
- JSON 构建：手动字符串拼接（避免引入 JSON 库）
- JSON 解析：正则表达式提取（简单场景足够）

## 错误处理

- API Key 未配置：引导用户前往配置页面
- 网络错误：显示详细错误信息
- API 返回错误：展示 HTTP 状态码和错误内容
- 无变更文件：提示无需生成

## 后续优化建议

1. **安全性增强**：使用 `PasswordSafe` 存储 API Key
2. **缓存机制**：对相同的 diff 内容缓存生成结果
3. **自定义 Prompt**：允许用户自定义 prompt 模板
4. **多语言支持**：支持生成英文或其他语言的提交信息
5. **流式响应**：支持 OpenAI 的流式 API，实时显示生成进度
6. **模型参数调优**：允许用户配置 temperature、max_tokens 等参数
7. **历史记录**：保存生成历史，支持快速选择之前的提交信息

## 测试

### 手动测试步骤

1. **配置测试**
   - 打开配置页面，填写正确的配置
   - 点击"测试连接"，应返回成功
   - 填写错误的 API Key，应返回错误提示

2. **生成测试**
   - 修改一些代码文件
   - 打开"变基并推送"对话框
   - 点击"AI生成"按钮
   - 验证生成的提交信息是否合理

3. **边界测试**
   - 无变更文件时点击"AI生成"：应提示无需生成
   - 未配置 API Key 时点击：应提示去配置
   - 网络异常时：应显示错误信息

## 版本信息

- 实现版本：v1.0.0
- 实现日期：2026-07-21
- 支持的 IntelliJ 版本：2025.3.5+