package com.examplecn.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.repo.GitRepository
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Merge Request创建服务
 * 自动从git remote配置提取平台信息，支持GitLab API自动创建MR
 */
@Service(Service.Level.PROJECT)
class MergeRequestService(private val project: Project) {

    private val passwordSafe = PasswordSafe.instance

    /**
     * 创建Merge Request
     */
    fun createMergeRequest(
        repository: GitRepository,
        sourceBranch: String,
        targetBranch: String
    ): MergeRequestResult {
        val remoteUrl = com.intellij.openapi.application.ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.remotes.firstOrNull { it.name == "origin" }?.firstUrl
            }) ?: return MergeRequestResult.NotConfigured("未找到origin远程仓库")

        val platform = detectPlatform(remoteUrl)

        return when (platform) {
            Platform.GITLAB -> createGitLabMR(remoteUrl, sourceBranch, targetBranch)
            Platform.GITHUB -> createGitHubPR(remoteUrl, sourceBranch, targetBranch)
        }
    }

    private fun detectPlatform(remoteUrl: String): Platform {
        return when {
            remoteUrl.contains("github.com", ignoreCase = true) -> Platform.GITHUB
            else -> Platform.GITLAB
        }
    }

    private fun createGitLabMR(
        remoteUrl: String,
        sourceBranch: String,
        targetBranch: String
    ): MergeRequestResult {
        val gitlabInfo = parseGitLabUrl(remoteUrl)
            ?: return MergeRequestResult.Error("无法解析GitLab URL: $remoteUrl")

        // 获取GitLab Token
        var token = getGitLabToken(gitlabInfo.baseUrl)

        // 如果没有token，提示用户输入
        if (token.isNullOrEmpty()) {
            token = promptForGitLabToken(gitlabInfo.baseUrl)
            if (token.isNullOrEmpty()) {
                val mrUrl = "${gitlabInfo.baseUrl}/${gitlabInfo.projectPath}/-/merge_requests/new?" +
                        "merge_request[source_branch]=${URLEncoder.encode(sourceBranch, "UTF-8")}&" +
                        "merge_request[target_branch]=${URLEncoder.encode(targetBranch, "UTF-8")}"
                return MergeRequestResult.NotConfigured(
                    "未配置GitLab Token，无法自动创建MR\n请点击以下链接手动创建:\n$mrUrl"
                )
            }
        }

        // 调用GitLab API创建MR
        return try {
            val projectId = URLEncoder.encode(gitlabInfo.projectPath, "UTF-8")
            val apiUrl = "${gitlabInfo.baseUrl}/api/v4/projects/$projectId/merge_requests"

            val title = "合并 $sourceBranch 到 $targetBranch"
            val description = "由IntelliJ IDEA Git插件自动创建的合并请求"

            val response = callGitLabAPI(apiUrl, token, sourceBranch, targetBranch, title, description)

            if (response.success) {
                MergeRequestResult.Success(response.mrUrl)
            } else {
                val mrUrl = "${gitlabInfo.baseUrl}/${gitlabInfo.projectPath}/-/merge_requests/new?" +
                        "merge_request[source_branch]=${URLEncoder.encode(sourceBranch, "UTF-8")}&" +
                        "merge_request[target_branch]=${URLEncoder.encode(targetBranch, "UTF-8")}"
                MergeRequestResult.Error(
                    "GitLab API调用失败: ${response.error}\n请点击以下链接手动创建:\n$mrUrl"
                )
            }
        } catch (e: Exception) {
            val mrUrl = "${gitlabInfo.baseUrl}/${gitlabInfo.projectPath}/-/merge_requests/new?" +
                    "merge_request[source_branch]=${URLEncoder.encode(sourceBranch, "UTF-8")}&" +
                    "merge_request[target_branch]=${URLEncoder.encode(targetBranch, "UTF-8")}"
            MergeRequestResult.Error(
                "创建MR时发生异常: ${e.message}\n请点击以下链接手动创建:\n$mrUrl"
            )
        }
    }

    private fun callGitLabAPI(
        apiUrl: String,
        token: String,
        sourceBranch: String,
        targetBranch: String,
        title: String,
        description: String
    ): GitLabAPIResponse {
        return try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("PRIVATE-TOKEN", token)
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            // 手动构建JSON字符串
            val jsonBody = buildJsonString(mapOf(
                "source_branch" to sourceBranch,
                "target_branch" to targetBranch,
                "title" to title,
                "description" to description
            ))

            // 发送请求
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode in 200..299) {
                // 成功创建
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                // 简单解析web_url
                val mrUrl = extractJsonValue(responseBody, "web_url")
                    ?: return GitLabAPIResponse(success = false, error = "无法解析响应中的web_url")
                GitLabAPIResponse(success = true, mrUrl = mrUrl)
            } else {
                // 失败
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                GitLabAPIResponse(success = false, error = "HTTP $responseCode: $errorBody")
            }
        } catch (e: Exception) {
            GitLabAPIResponse(success = false, error = e.message ?: "Unknown error")
        }
    }

    private fun buildJsonString(data: Map<String, String>): String {
        val entries = data.entries.joinToString(",") { (key, value) ->
            val escapedValue = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            "\"$key\":\"$escapedValue\""
        }
        return "{$entries}"
    }

    private fun extractJsonValue(json: String, key: String): String? {
        // 简单的JSON值提取，匹配 "key":"value"
        val pattern = Regex(""""$key"\s*:\s*"([^"]*?)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun createGitHubPR(
        remoteUrl: String,
        sourceBranch: String,
        targetBranch: String
    ): MergeRequestResult {
        val repoPath = parseGitHubRepoPath(remoteUrl)
            ?: return MergeRequestResult.Error("无法解析GitHub URL: $remoteUrl")

        val prUrl = "https://github.com/$repoPath/compare/$targetBranch...$sourceBranch"

        return MergeRequestResult.NotConfigured(
            "GitHub PR自动创建功能暂未实现\n请点击以下链接手动创建:\n$prUrl"
        )
    }

    private fun getGitLabToken(baseUrl: String): String? {
        val credentialAttributes = createCredentialAttributes(baseUrl)
        return passwordSafe.getPassword(credentialAttributes)
    }

    private fun saveGitLabToken(baseUrl: String, token: String) {
        val credentialAttributes = createCredentialAttributes(baseUrl)
        val credentials = Credentials("gitlab-token", token)
        passwordSafe.set(credentialAttributes, credentials)
    }

    private fun createCredentialAttributes(baseUrl: String): CredentialAttributes {
        val serviceName = generateServiceName("GitLabToken", baseUrl)
        return CredentialAttributes(serviceName, "gitlab-token")
    }

    private fun promptForGitLabToken(baseUrl: String): String? {
        var token: String? = null
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
            token = Messages.showInputDialog(
                project,
                "请输入GitLab Personal Access Token\n" +
                "Token需要api权限，可在以下链接创建:\n" +
                "$baseUrl/-/user_settings/personal_access_tokens\n\n" +
                "Token将安全存储在系统密钥链中",
                "GitLab Token配置",
                Messages.getQuestionIcon()
            )
        }

        if (!token.isNullOrEmpty()) {
            saveGitLabToken(baseUrl, token!!)
        }

        return token
    }

    private fun parseGitLabUrl(remoteUrl: String): GitLabInfo? {
        val sshPattern = Regex("""git@([^:]+):(.+?)(?:\.git)?$""")
        val httpsPattern = Regex("""https?://([^/]+)/(.+?)(?:\.git)?$""")

        val sshMatch = sshPattern.find(remoteUrl)
        if (sshMatch != null) {
            val host = sshMatch.groupValues[1]
            val projectPath = sshMatch.groupValues[2]
            return GitLabInfo(baseUrl = "https://$host", projectPath = projectPath)
        }

        val httpsMatch = httpsPattern.find(remoteUrl)
        if (httpsMatch != null) {
            val host = httpsMatch.groupValues[1]
            val projectPath = httpsMatch.groupValues[2]
            return GitLabInfo(baseUrl = "https://$host", projectPath = projectPath)
        }

        return null
    }

    private fun parseGitHubRepoPath(remoteUrl: String): String? {
        val pattern = Regex("""(?:github\.com[:/])(.+?)(?:\.git)?$""")
        return pattern.find(remoteUrl)?.groupValues?.get(1)
    }

    private data class GitLabInfo(
        val baseUrl: String,
        val projectPath: String
    )

    private data class GitLabAPIResponse(
        val success: Boolean,
        val mrUrl: String = "",
        val error: String = ""
    )

    private enum class Platform {
        GITLAB, GITHUB
    }
}

sealed class MergeRequestResult {
    data class Success(val url: String) : MergeRequestResult()
    data class NotConfigured(val reason: String) : MergeRequestResult()
    data class Error(val message: String) : MergeRequestResult()
}