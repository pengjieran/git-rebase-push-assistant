# OpenAI Configuration Examples

## Official OpenAI

```
Base URL: https://api.openai.com/v1
Model ID: gpt-4o-mini
API Key: sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

推荐模型：
- `gpt-4o-mini` - 最经济实惠，速度快，适合日常使用
- `gpt-4o` - 更强大的推理能力
- `gpt-4-turbo` - 平衡性能和成本
- `gpt-3.5-turbo` - 最便宜，基础功能

## Azure OpenAI

```
Base URL: https://your-resource.openai.azure.com/openai/deployments/your-deployment
Model ID: gpt-4
API Key: your-azure-api-key
```

注意：Azure OpenAI 的 URL 格式不同，需要包含部署名称。

## 国内服务商示例

### 通义千问（如果支持 OpenAI 格式）

```
Base URL: https://dashscope.aliyuncs.com/compatible-mode/v1
Model ID: qwen-turbo
API Key: sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 智谱 AI (ChatGLM)

```
Base URL: https://open.bigmodel.cn/api/paas/v4
Model ID: glm-4
API Key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.xxxxxxxx
```

### DeepSeek

```
Base URL: https://api.deepseek.com/v1
Model ID: deepseek-chat
API Key: sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

## 本地部署

### Ollama (需要 OpenAI 兼容层)

```
Base URL: http://localhost:11434/v1
Model ID: llama2
API Key: ollama
```

注意：Ollama 默认不提供 OpenAI 兼容 API，需要使用 litellm 等工具做转换。

### LM Studio

```
Base URL: http://localhost:1234/v1
Model ID: local-model
API Key: not-needed
```

LM Studio 提供内置的 OpenAI 兼容服务器。

## API Key 获取方式

### OpenAI
1. 访问 https://platform.openai.com/api-keys
2. 点击 "Create new secret key"
3. 复制生成的密钥（只显示一次）

### Azure OpenAI
1. 访问 Azure Portal
2. 进入你的 OpenAI 资源
3. 在 "Keys and Endpoint" 页面获取密钥

## 安全建议

1. **不要提交 API Key 到版本控制系统**
   - 配置文件 `gitRebasePlugin.xml` 应添加到 `.gitignore`

2. **使用环境变量**
   - 考虑从环境变量读取 API Key（需要代码修改）

3. **定期轮换密钥**
   - 建议每 3-6 个月更换一次 API Key

4. **监控使用量**
   - 定期检查 API 使用情况，避免意外超支

5. **设置使用限制**
   - 在 OpenAI 控制台设置月度使用限额

## 成本估算

基于 OpenAI 官方定价（2024）：

### 每次生成成本估算
- 输入：约 4000 tokens（文件列表 + diff）
- 输出：约 50 tokens（提交信息）
- 使用 gpt-4o-mini：
  - 输入：$0.15 / 1M tokens = $0.0006
  - 输出：$0.60 / 1M tokens = $0.00003
  - **单次成本：约 $0.00063 (不到 1 美分)**

### 月度成本估算
假设每天生成 20 次提交信息：
- 每天：$0.00063 × 20 = $0.0126
- 每月：$0.0126 × 30 = **$0.378**

非常经济实惠！

## 故障排查

### 连接失败
```
错误：Connection timeout
解决：检查网络连接，确认 Base URL 可访问
```

### 401 Unauthorized
```
错误：HTTP 401
解决：检查 API Key 是否正确，是否已过期
```

### 429 Too Many Requests
```
错误：HTTP 429
解决：API 调用频率过高，稍后重试或升级配额
```

### 模型不存在
```
错误：model not found
解决：检查模型 ID 是否正确，确认账户是否有权限使用该模型
```

### 响应解析失败
```
错误：无法解析响应
解决：检查服务商是否完全兼容 OpenAI API 格式
```

## 高级配置（需要代码修改）

当前实现使用固定参数，如需自定义可修改 `OpenAIService.kt`：

```kotlin
// 当前固定参数
"temperature": 0.7,
"max_tokens": 200

// 可配置选项
temperature: 0.0-2.0（创造性程度，0 更确定，2 更随机）
max_tokens: 生成的最大 token 数
top_p: 核采样参数
frequency_penalty: 频率惩罚
presence_penalty: 存在惩罚
```

## 参考链接

- [OpenAI API 文档](https://platform.openai.com/docs/api-reference)
- [OpenAI 定价](https://openai.com/api/pricing/)
- [Azure OpenAI 文档](https://learn.microsoft.com/en-us/azure/ai-services/openai/)