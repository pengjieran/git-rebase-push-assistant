
package com.examplecn.action

import com.examplecn.service.GitRebaseService
import com.examplecn.service.MergeRequestResult
import com.examplecn.service.MergeRequestService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel

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

        val configDialog = RebaseConfigDialog(project, repository)
        if (!configDialog.showAndGet()) return

        executeRebaseAndPushInBackground(
            project,
            repository,
            configDialog.selectedBranch,
            configDialog.shouldCreateMR
        )
    }

    private fun executeRebaseAndPushInBackground(
        project: Project,
        repository: GitRepository,
        targetBranch: String,
        createMR: Boolean
    ) {
        val currentBranch = repository.currentBranch?.name ?: run {
            Messages.showErrorDialog(project, "未找到当前分支", "错误")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "正在将 $currentBranch 变基到 $targetBranch",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val service = project.service<GitRebaseService>()

                try {
                    indicator.text = "检查未提交的变更..."
                    indicator.fraction = 0.05
                    val changedFiles = service.getChangedFiles(repository)

                    if (changedFiles.isNotEmpty()) {
                        indicator.text = "等待选择提交文件..."
                        var shouldProceed = false
                        var filesToCommit: List<String> = emptyList()
                        var commitMessage = ""

                        com.intellij.openapi.application.ApplicationManager.getApplication()
                            .invokeAndWait {
                                val commitDialog = CommitFilesDialog(project, changedFiles)
                                shouldProceed = commitDialog.showAndGet()
                                if (shouldProceed) {
                                    filesToCommit = commitDialog.selectedFiles
                                    commitMessage = commitDialog.commitMessage
                                }
                            }

                        if (!shouldProceed) {
                            return
                        }

                        if (filesToCommit.isNotEmpty()) {
                            indicator.text = "正在提交选中的文件..."
                            indicator.fraction = 0.15
                            service.addFiles(repository, filesToCommit)
                            service.commitChanges(repository, commitMessage)
                        }
                    }

                    indicator.text = "正在拉取远程分支 $targetBranch..."
                    indicator.fraction = 0.35
                    service.fetchRemoteBranch(repository, targetBranch)

                    indicator.text = "正在将 $currentBranch 变基到 $targetBranch..."
                    indicator.fraction = 0.55
                    service.rebaseOnto(repository, targetBranch)

                    indicator.text = "正在推送到远程仓库..."
                    indicator.fraction = 0.75
                    service.forcePushBranch(repository, currentBranch)

                    indicator.fraction = 1.0

                    if (createMR) {
                        createMergeRequest(project, repository, currentBranch, targetBranch)
                    } else {
                        com.intellij.openapi.application.ApplicationManager.getApplication()
                            .invokeLater {
                                Messages.showInfoMessage(
                                    project,
                                    "已成功将 $currentBranch 变基到 $targetBranch 并推送到远程仓库",
                                    "变基完成"
                                )
                            }
                    }
                } catch (e: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication()
                        .invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "变基失败: ${e.message}",
                                "错误"
                            )
                        }
                }
            }
        })
    }

    private fun createMergeRequest(
        project: Project,
        repository: GitRepository,
        sourceBranch: String,
        targetBranch: String
    ) {
        val mrService = project.service<MergeRequestService>()
        val result = mrService.createMergeRequest(repository, sourceBranch, targetBranch)

        com.intellij.openapi.application.ApplicationManager.getApplication()
            .invokeLater {
                when (result) {
                    is MergeRequestResult.Success -> {
                        Messages.showInfoMessage(
                            project,
                            "变基、提交和推送已完成\n\nMerge请求已创建:\n${result.url}",
                            "变基完成"
                        )
                    }
                    is MergeRequestResult.NotConfigured -> {
                        Messages.showWarningDialog(
                            project,
                            "变基、提交和推送已完成，但Merge请求未创建:\n${result.reason}\n\n" +
                                    "请在 设置 > 工具 > Git变基插件 中配置对应平台的访问凭证",
                            "Merge请求未创建"
                        )
                    }
                    is MergeRequestResult.Error -> {
                        Messages.showErrorDialog(
                            project,
                            "变基、提交和推送已完成，但Merge请求创建失败:\n${result.message}",
                            "Merge请求失败"
                        )
                    }
                }
            }
    }
}

class RebaseConfigDialog(
    private val project: Project,
    private val repository: GitRepository
) : DialogWrapper(project) {

    private val branchField: TextFieldWithAutoCompletion<String>
    private val createMRCheckBox: JBCheckBox
    private val branches: List<String>

    var selectedBranch: String = ""
        private set
    var shouldCreateMR: Boolean = false
        private set

    init {
        title = "变基并推送配置"

        val service = project.service<GitRebaseService>()
        branches = service.getRemoteBranches(repository)

        val currentBranch = repository.currentBranch?.name
        val suggestedBranch = when {
            branches.contains("master") && currentBranch != "master" -> "master"
            branches.contains("main") && currentBranch != "main" -> "main"
            branches.contains("develop") && currentBranch != "develop" -> "develop"
            else -> branches.firstOrNull { it != currentBranch }
        }

        branchField = TextFieldWithAutoCompletion.create(
            project,
            branches,
            false,
            suggestedBranch ?: ""
        )
        branchField.setPreferredWidth(300)

        createMRCheckBox = JBCheckBox("推送后自动提交merge请求", false)

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))

        val formPanel = JPanel(GridLayout(0, 2, 5, 10))
        formPanel.add(JBLabel("目标分支:"))
        formPanel.add(branchField)
        formPanel.add(JBLabel(""))
        formPanel.add(createMRCheckBox)

        panel.add(formPanel, BorderLayout.CENTER)

        val infoPanel = JPanel(BorderLayout())
        infoPanel.add(
            JBLabel("<html><i>当前分支会变基到您选择的目标分支<br>" +
                    "变更将使用 --force-with-lease 强制推送</i></html>"),
            BorderLayout.CENTER
        )
        panel.add(infoPanel, BorderLayout.SOUTH)

        return panel
    }

    override fun doOKAction() {
        selectedBranch = branchField.text.trim()
        if (selectedBranch.isEmpty()) {
            Messages.showErrorDialog(project, "请选择一个目标分支", "错误")
            return
        }

        if (!branches.contains(selectedBranch)) {
            Messages.showErrorDialog(
                project,
                "分支 \"$selectedBranch\" 不存在，请从建议列表中选择一个有效的分支",
                "错误"
            )
            return
        }

        val currentBranch = repository.currentBranch?.name
        if (selectedBranch == currentBranch) {
            Messages.showErrorDialog(
                project,
                "不能变基到当前分支",
                "错误"
            )
            return
        }

        shouldCreateMR = createMRCheckBox.isSelected
        super.doOKAction()
    }
}