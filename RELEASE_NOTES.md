# 新功能发布：OpenAI 智能提交信息生成

## 🎉 更新内容

本次更新为 Git Rebase & Push 插件添加了 AI 驱动的提交信息自动生成功能。

## ✨ 核心功能

### 1. 配置界面
- 在 `Settings → Tools → Git Rebase & Push` 中配置
- 支持配置 OpenAI Base URL、模型 ID 和 API Key
- 内置连接测试功能

### 2. AI 生成按钮
- 在变基对话框的提交信息区域新增"AI生成"按钮
- 一键根据代码变更自动生成提交信息
- 智能分析变更内容，生成符合规范的中文提交信息

### 3. 兼容性
- 支持 OpenAI 官方 API
- 支持 Azure OpenAI
- 支持所有 OpenAI 兼容的第三方服务
- 无需外部依赖，使用纯 JDK 实现

## 🚀 快速开始

### 第一步：配置

1. 打开 IntelliJ IDEA
2. 进入 `Settings/Preferences → Tools → Git Rebase & Push`
3. 填写配置：
   ```
   Base URL: https://api.openai.com/v1
   模型ID: gpt-4o-mini
   API Key: sk-your-api-key
   ```
4. 点击"测试连接"验证

### 第二步：使用

1. 修改代码
2. 打开 Git 提交界面，点击"变基并提交推送"
3. 在对话框中点击"AI生成"按钮（带灯泡图标）
4. 等待 AI 生成提交信息
5. 根据需要微调，然后提交

## 📁 文件结构

```
src/main/kotlin/com/examplecn/
├── config/
│   ├── GitRebaseSettings.kt                    # 扩展：新增 OpenAI 配置字段
│   └── GitRebaseSettingsConfigurable.kt        # 新增：配置界面
├── service/
│   ├── GitRebaseService.kt                     # 扩展：新增 getDiff() 方法
│   └── OpenAIService.kt                        # 新增：OpenAI API 调用服务
└── action/
    └── UnifiedRebaseDialog.kt                  # 扩展：新增 AI 生成按钮
```

## 📚 文档

- [功能详细说明](OPENAI_FEATURE.md)
- [配置示例](OPENAI_CONFIG_EXAMPLES.md)

## 🔧 技术实现

- **HTTP 客户端**: `java.net.HttpURLConnection`（纯 JDK，无外部依赖）
- **JSON 处理**: 手动构建和正则解析（避免引入 JSON 库）
- **线程模型**: 后台线程调用 API，UI 线程更新界面
- **错误处理**: 完整的异常捕获和用户友好的错误提示

## 💰 成本

使用 `gpt-4o-mini` 模型：
- 单次生成：< $0.001（不到 1 美分）
- 月度成本（每天 20 次）：约 $0.38

非常经济实惠！

## 🛡️ 安全性

- API Key 存储在项目配置文件中
- 配置界面使用密码框遮蔽显示
- 建议：添加到 `.gitignore` 避免泄露

## ⚡ 性能

- 异步调用，不阻塞 UI
- 典型响应时间：2-5 秒
- diff 内容截断至 4000 字符以控制成本

## 🐛 已知问题

无

## 📝 后续计划

1. 使用 `PasswordSafe` 安全存储 API Key
2. 支持自定义 Prompt 模板
3. 支持多语言提交信息
4. 添加生成历史记录
5. 支持流式响应

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可

与主项目相同

## 📞 联系

有问题或建议？请在 Issues 中反馈。

---

**版本**: v1.1.0 (OpenAI Integration)  
**日期**: 2026-07-21  
**构建状态**: ✅ 通过