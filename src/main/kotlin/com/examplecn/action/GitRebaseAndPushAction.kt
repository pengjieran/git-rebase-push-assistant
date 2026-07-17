
package com.examplecn.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import git4idea.GitUtil

class GitRebaseAndPushAction : AnAction("变基并提交推送") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                GitUtil.getRepositoryManager(project).repositories.isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repositories = GitUtil.getRepositoryManager(project).repositories
        val repository = repositories.firstOrNull() ?: run {
            Messages.showErrorDialog(project, "未找到Git仓库", "错误")
            return
        }

        val dialog = UnifiedRebaseDialog(project, repository)
        dialog.show()
    }
}