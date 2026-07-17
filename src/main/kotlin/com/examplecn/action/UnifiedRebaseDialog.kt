package com.examplecn.action

import com.examplecn.service.GitRebaseService
import com.examplecn.service.MergeRequestResult
import com.examplecn.service.MergeRequestService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class UnifiedRebaseDialog(
    private val project: Project,
    private val repository: GitRepository
) : DialogWrapper(project) {

    private val service = project.service<GitRebaseService>()
    private val branches: List<String>
    private val changedFiles: List<String>

    private val branchField: TextFieldWithAutoCompletion<String>
    private val messageArea = JBTextArea(4, 50)
    private val createMRCheckBox: JBCheckBox
    private val filesListLabel: JBLabel
    private val progressPanel: JPanel
    private val progressTextArea: JBTextArea

    var selectedBranch: String = ""
        private set
    var commitMessage: String = ""
        private set
    var shouldCreateMR: Boolean = false
        private set

    init {
        title = "变基并推送"

        branches = service.getRemoteBranches(repository)
        changedFiles = service.getChangedFiles(repository)

        val currentBranch = com.intellij.openapi.application.ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.currentBranch?.name
            })

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
        branchField.setPreferredWidth(400)

        createMRCheckBox = JBCheckBox("推送后自动提交merge请求", false)
        filesListLabel = JBLabel()

        progressPanel = JPanel(BorderLayout())
        progressPanel.isVisible = false
        progressTextArea = JBTextArea(10, 50)
        progressTextArea.isEditable = false
        progressTextArea.lineWrap = true
        progressTextArea.wrapStyleWord = true

        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 10))
        mainPanel.preferredSize = Dimension(600, 500)
        mainPanel.border = JBUI.Borders.empty(10)

        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 5)

        // 目标分支
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        val branchLabel = JBLabel("目标分支:")
        branchLabel.font = branchLabel.font.deriveFont(java.awt.Font.BOLD)
        formPanel.add(branchLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        formPanel.add(branchField, gbc)

        // 变动文件
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        val filesLabel = JBLabel("变动文件:")
        filesLabel.font = filesLabel.font.deriveFont(java.awt.Font.BOLD)
        formPanel.add(filesLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        val filesInfo = if (changedFiles.isEmpty()) {
            "无变动文件"
        } else {
            "${changedFiles.size} 个文件 (全部默认提交)"
        }
        filesListLabel.text = "<html><body>${filesInfo}<br><small style='color: gray;'>${
            changedFiles.take(5).joinToString("<br>")
        }${if (changedFiles.size > 5) "<br>..." else ""}</small></body></html>"
        formPanel.add(filesListLabel, gbc)

        // 提交信息
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.NORTHWEST
        val messageLabel = JBLabel("提交信息:")
        messageLabel.font = messageLabel.font.deriveFont(java.awt.Font.BOLD)
        formPanel.add(messageLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.weighty = 0.3
        gbc.fill = GridBagConstraints.BOTH
        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.border = JBUI.Borders.empty(4)
        val messageScroll = JBScrollPane(messageArea)
        messageScroll.preferredSize = Dimension(400, 100)
        formPanel.add(messageScroll, gbc)

        // MR选项
        gbc.gridx = 1
        gbc.gridy = 3
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        formPanel.add(createMRCheckBox, gbc)

        mainPanel.add(formPanel, BorderLayout.NORTH)

        // 进度面板
        val progressLabel = JBLabel("执行进度:")
        progressLabel.font = progressLabel.font.deriveFont(java.awt.Font.BOLD)
        progressLabel.border = JBUI.Borders.emptyBottom(5)
        progressPanel.add(progressLabel, BorderLayout.NORTH)
        progressTextArea.border = JBUI.Borders.empty(4)
        progressTextArea.background = java.awt.Color(245, 245, 245)
        val progressScroll = JBScrollPane(progressTextArea)
        progressScroll.preferredSize = Dimension(580, 150)
        progressPanel.add(progressScroll, BorderLayout.CENTER)
        progressPanel.border = JBUI.Borders.emptyTop(10)

        mainPanel.add(progressPanel, BorderLayout.CENTER)

        return mainPanel
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

        val currentBranch = com.intellij.openapi.application.ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.currentBranch?.name
            })

        if (selectedBranch == currentBranch) {
            Messages.showErrorDialog(project, "不能变基到当前分支", "错误")
            return
        }

        if (changedFiles.isNotEmpty() && messageArea.text.isBlank()) {
            Messages.showErrorDialog(project, "有变动文件时必须填写提交信息", "错误")
            return
        }

        commitMessage = messageArea.text.trim()
        shouldCreateMR = createMRCheckBox.isSelected

        // 禁用按钮，防止重复提交
        okAction.isEnabled = false
        cancelAction.isEnabled = false

        // 显示进度面板并开始执行
        progressPanel.isVisible = true
        window.pack()

        executeRebaseWorkflow(currentBranch ?: "")
    }

    private fun executeRebaseWorkflow(currentBranch: String) {
        try {
            if (changedFiles.isNotEmpty()) {
                appendProgress("检查未提交的变更...")
                appendProgress("发现 ${changedFiles.size} 个变动文件，准备提交\n")

                appendProgress("正在添加文件到暂存区...")
                service.addFiles(repository, changedFiles)
                appendProgress("✓ 文件已添加到暂存区\n")

                appendProgress("正在提交变更...")
                service.commitChanges(repository, commitMessage)
                appendProgress("✓ 变更已提交\n")
            }

            appendProgress("正在拉取远程分支 $selectedBranch...")
            service.fetchRemoteBranch(repository, selectedBranch)
            appendProgress("✓ 远程分支已拉取\n")

            appendProgress("正在将 $currentBranch 变基到 $selectedBranch...")
            service.rebaseOnto(repository, selectedBranch)
            appendProgress("✓ 变基成功\n")

            appendProgress("正在推送到远程仓库...")
            service.forcePushBranch(repository, currentBranch)
            appendProgress("✓ 推送成功\n")

            if (shouldCreateMR) {
                appendProgress("\n正在创建Merge请求...")
                createMergeRequest(currentBranch)
            } else {
                appendProgress("\n✅ 所有操作已完成")
                showSuccessAndClose()
            }

        } catch (e: Exception) {
            appendProgress("\n❌ 错误: ${e.message}")
            Messages.showErrorDialog(project, "操作失败: ${e.message}", "错误")
            close(CLOSE_EXIT_CODE)
        }
    }

    private fun appendProgress(text: String) {
        SwingUtilities.invokeLater {
            progressTextArea.append(text + "\n")
            progressTextArea.caretPosition = progressTextArea.document.length
        }
    }

    private fun createMergeRequest(sourceBranch: String) {
        val mrService = project.service<MergeRequestService>()
        val result = mrService.createMergeRequest(
            repository,
            sourceBranch,
            selectedBranch
        )

        when (result) {
            is MergeRequestResult.Success -> {
                appendProgress("✓ Merge请求已创建")
                appendProgress("URL: ${result.url}")
                appendProgress("\n✅ 所有操作已完成")
                showSuccessAndClose()
            }
            is MergeRequestResult.NotConfigured -> {
                appendProgress("⚠ Merge请求未创建: ${result.reason}")
                appendProgress("请在 设置 > 工具 > Git变基插件 中配置对应平台的访问凭证")
                appendProgress("\n✅ 变基和推送已完成")
                showSuccessAndClose()
            }
            is MergeRequestResult.Error -> {
                appendProgress("❌ Merge请求创建失败: ${result.message}")
                appendProgress("\n✅ 变基和推送已完成")
                showSuccessAndClose()
            }
        }
    }

    private fun showSuccessAndClose() {
        SwingUtilities.invokeLater {
            Timer(2000) {
                close(OK_EXIT_CODE)
            }.apply {
                isRepeats = false
                start()
            }
        }
    }
}