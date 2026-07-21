package com.examplecn.service

import com.examplecn.config.GitRebaseSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * OpenAI服务，用于调用API生成提交信息
 */
@Service(Service.Level.PROJECT)
class OpenAIService(private val project: Project) {

    /**
     * 根据变更内容生成提交信息
     */
    fun generateCommitMessage(changedFiles: List<String>, diff: String): Result<String> {
        val settings = GitRebaseSettings.getInstance(project).state

        if (settings.openaiApiKey.isBlank()) {
            return Result.failure(Exception("OpenAI API Key未配置"))
        }

        try {
            val prompt = buildPrompt(changedFiles, diff)
            val response = callOpenAI(
                settings.openaiBaseUrl,
                settings.openaiApiKey,
                settings.openaiModel,
                prompt
            )
            return Result.success(response)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * 测试OpenAI连接
     */
    fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API Key不能为空"))
        }

        try {
            val response = callOpenAI(
                baseUrl,
                apiKey,
                model,
                "Say 'Connection successful' in Chinese"
            )
            return Result.success(response)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun buildPrompt(changedFiles: List<String>, diff: String): String {
        val filesInfo = changedFiles.joinToString("\n") { "- $it" }

        return """你是一个Git提交信息生成助手。请根据以下代码变更生成简洁、清晰的中文提交信息。

变更的文件：
$filesInfo

代码变更内容（diff）：
${diff.take(4000)}

要求：
1. 使用中文
2. 简洁明了，一行为主，不超过50个字符
3. 使用动词开头（如：修复、增加、优化、重构）
4. 如果变更较多，可以用一句话概括主要目的
5. 不要包含代码细节
6. 直接返回提交信息内容，不要有任何前缀或解释

提交信息："""
    }

    private fun callOpenAI(baseUrl: String, apiKey: String, model: String, prompt: String): String {
        val url = URL("$baseUrl/chat/completions")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val requestBody = buildRequestJson(model, prompt)

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = BufferedReader(InputStreamReader(errorStream, StandardCharsets.UTF_8)).use {
                    it.readText()
                }

                // 提供更友好的错误提示
                val friendlyMessage = when (responseCode) {
                    502, 503, 504 -> "OpenAI服务暂时不可用（HTTP $responseCode），请稍后重试"
                    401 -> "API Key无效或已过期"
                    429 -> "请求过于频繁，请稍后重试"
                    500 -> "OpenAI服务器内部错误"
                    else -> "请求失败（HTTP $responseCode）"
                }

                throw Exception("$friendlyMessage\n详细信息: $errorBody")
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use {
                it.readText()
            }

            return parseResponse(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequestJson(model: String, prompt: String): String {
        // 手动构建JSON以避免引入外部依赖
        val escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        return """
{
  "model": "$model",
  "messages": [
    {
      "role": "user",
      "content": "$escapedPrompt"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 200
}
        """.trim()
    }

    private fun parseResponse(json: String): String {
        // 简单的JSON解析，提取choices[0].message.content
        // 格式：{"choices":[{"message":{"content":"..."}}]}

        // 先尝试提取content字段
        val contentPattern = """"content"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
        val match = contentPattern.find(json)

        if (match == null) {
            // 如果无法解析，检查是否是错误响应
            val errorPattern = """"error"\s*:\s*\{[^}]*"message"\s*:\s*"([^"]*)"""".toRegex()
            val errorMatch = errorPattern.find(json)

            if (errorMatch != null) {
                val errorMsg = errorMatch.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                throw Exception("API错误: $errorMsg\n\n原始响应:\n${json.take(500)}")
            }

            // 既不是成功响应也不是标准错误格式，显示原始内容
            throw Exception("无法解析响应格式\n\n原始响应:\n${json.take(500)}")
        }

        val content = match.groupValues[1]
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .trim()

        // 直接返回OpenAI的原始内容
        return content
    }

    companion object {
        fun getInstance(project: Project): OpenAIService {
            return project.getService(OpenAIService::class.java)
        }
    }
}