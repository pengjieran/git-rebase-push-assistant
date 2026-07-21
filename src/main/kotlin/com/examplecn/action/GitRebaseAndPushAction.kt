package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import git4idea.GitUtil

class GitRebaseAndPushAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                GitUtil.getRepositoryManager(project).repositories.isNotEmpty()
        e.presentation.text = GitRebaseBundle.message("action.name")
        e.presentation.description = GitRebaseBundle.message("action.description")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repositories = GitUtil.getRepositoryManager(project).repositories
        val repository = repositories.firstOrNull() ?: run {
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("error.no.repository"),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        // 尝试从提交框架获取已填写的提交信息
        val commitMessage = extractCommitMessage(e)

        // Load changed files in background before showing dialog to avoid EDT blocking
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : com.intellij.openapi.progress.Task.Backgroundable(
                project,
                GitRebaseBundle.message("action.name"),
                false
            ) {
                private var changedFiles: List<String> = emptyList()
                private var branches: List<String> = emptyList()

                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    indicator.text = "Loading repository data..."
                    val service = project.service<com.examplecn.service.GitRebaseService>()
                    changedFiles = service.getChangedFiles(repository)
                    branches = service.getRemoteBranches(repository)
                }

                override fun onSuccess() {
                    val dialog = UnifiedRebaseDialog(project, repository, commitMessage, changedFiles, branches)
                    dialog.show()
                }

                override fun onThrowable(error: Throwable) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to load repository data: ${error.message}",
                        GitRebaseBundle.message("error.title")
                    )
                }
            }
        )
    }

    /**
     * 从AnActionEvent中提取提交信息
     * 该动作在 Vcs.CommitExecutor.Actions 组中，可以访问提交上下文
     */
    private fun extractCommitMessage(e: AnActionEvent): String? {
        try {
            // 尝试从 VcsDataKeys.COMMIT_MESSAGE_CONTROL 获取
            // 返回类型可能是 EditorTextField 或其他类型
            val commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) ?: return null

            // 使用反射调用 getText() 方法
            val getTextMethod = commitMessageControl.javaClass.getMethod("getText")
            val text = getTextMethod.invoke(commitMessageControl) as? String

            return text?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            // 如果API不兼容或反射失败，返回null
            return null
        }
    }
}