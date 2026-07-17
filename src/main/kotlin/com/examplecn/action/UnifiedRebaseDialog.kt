package com.examplecn.action

import com.examplecn.config.GitRebaseSettings
import com.examplecn.service.GitRebaseService
import com.examplecn.service.MergeRequestResult
import com.examplecn.service.MergeRequestService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
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
    private val formPanel: JPanel

    private val appendWebhookCheckBox: JBCheckBox
    private val appendJiraCheckBox: JBCheckBox
    private val jiraNumberField: JTextField
    private val addJiraButton: JButton

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

        val currentBranch = ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.currentBranch?.name
            })

        val defaultTargetBranch = GitRebaseSettings.getInstance(project).state.defaultTargetBranch
        val suggestedBranch = when {
            branches.contains(defaultTargetBranch) && currentBranch != defaultTargetBranch -> defaultTargetBranch
            branches.contains("master") && currentBranch != "master" -> "master"
            branches.contains("main") && currentBranch != "main" -> "main"
            branches.contains("develop") && currentBranch != "develop" -> "develop"
            else -> branches.firstOrNull { it != currentBranch }
        }

        val provider = FuzzyBranchCompletionProvider(branches)
        branchField = TextFieldWithAutoCompletion(
            project,
            provider,
            false,
            suggestedBranch ?: ""
        )
        branchField.setPreferredWidth(400)

        createMRCheckBox = JBCheckBox("推送后自动提交merge请求", false)
        filesListLabel = JBLabel()

        appendWebhookCheckBox = JBCheckBox("自动追加 #webhook", false)
        appendJiraCheckBox = JBCheckBox("追加JIRA编号", false)
        jiraNumberField = JTextField(15)
        addJiraButton = JButton("添加")
        addJiraButton.addActionListener { appendJiraNumber() }

        jiraNumberField.isEnabled = false
        addJiraButton.isEnabled = false
        appendJiraCheckBox.addItemListener {
            val enabled = appendJiraCheckBox.isSelected
            jiraNumberField.isEnabled = enabled
            addJiraButton.isEnabled = enabled
        }

        formPanel = JPanel(GridBagLayout())

        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 10))
        mainPanel.preferredSize = Dimension(600, 400)
        mainPanel.border = JBUI.Borders.empty(10)

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
        val filesSection = JPanel(BorderLayout(0, 6))

        val filesInfo = if (changedFiles.isEmpty()) {
            "无变动文件"
        } else {
            "${changedFiles.size} 个文件 (全部默认提交)"
        }
        filesListLabel.text = "<html><body>${filesInfo}<br><small style='color: gray;'>${
            changedFiles.take(5).joinToString("<br>")
        }${if (changedFiles.size > 5) "<br>..." else ""}</small></body></html>"
        filesSection.add(filesListLabel, BorderLayout.NORTH)

        if (changedFiles.isNotEmpty()) {
            val warningBanner = JBLabel(
                "<html><body style='width: 350px; padding: 2px 0;'>" +
                        "变基时不允许有部分未提交的文件,所有变更文件将被自动提交" +
                        "</body></html>"
            )
            warningBanner.foreground = java.awt.Color(0xB3, 0x66, 0x00)
            warningBanner.isOpaque = true
            warningBanner.background = java.awt.Color(0xFF, 0xF4, 0xE0)
            warningBanner.border = JBUI.Borders.empty(4, 6)
            filesSection.add(warningBanner, BorderLayout.SOUTH)
        }

        formPanel.add(filesSection, gbc)

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

        // 自动追加 #webhook
        gbc.gridx = 1
        gbc.gridy = 3
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        formPanel.add(appendWebhookCheckBox, gbc)

        // JIRA编号追加
        gbc.gridx = 1
        gbc.gridy = 4
        val jiraPanel = JPanel()
        jiraPanel.layout = BoxLayout(jiraPanel, BoxLayout.X_AXIS)
        jiraPanel.add(appendJiraCheckBox)
        jiraPanel.add(Box.createHorizontalStrut(6))
        jiraPanel.add(jiraNumberField)
        jiraPanel.add(Box.createHorizontalStrut(6))
        jiraPanel.add(addJiraButton)
        formPanel.add(jiraPanel, gbc)

        // MR选项
        gbc.gridx = 1
        gbc.gridy = 5
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        formPanel.add(createMRCheckBox, gbc)

        mainPanel.add(formPanel, BorderLayout.CENTER)

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

        val currentBranch = ApplicationManager.getApplication()
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

        // 处理 #webhook 追加
        if (appendWebhookCheckBox.isSelected && commitMessage.isNotEmpty()) {
            commitMessage += " #webhook"
        }

        // 立即关闭对话框，在后台执行
        super.doOKAction()

        executeRebaseWorkflow(currentBranch ?: "")
    }

    override fun doCancelAction() {
        super.doCancelAction()
    }

    private fun appendJiraNumber() {
        val jiraNumber = jiraNumberField.text.trim()
        if (jiraNumber.isEmpty()) {
            return
        }

        val tag = "\$(JIRA:$jiraNumber)"
        val current = messageArea.text
        val separator = if (current.isNotEmpty() && !current.endsWith(";") && !current.endsWith("\n")) ";" else ""
        messageArea.text = current + separator + tag

        jiraNumberField.text = ""
    }

    private fun executeRebaseWorkflow(currentBranch: String) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "变基并推送到 $selectedBranch", false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        if (changedFiles.isNotEmpty()) {
                            indicator.text = "提交变更..."
                            indicator.text2 = "发现 ${changedFiles.size} 个变动文件"

                            service.addFiles(repository, changedFiles)
                            service.commitChanges(repository, commitMessage)
                        }

                        indicator.text = "拉取远程分支..."
                        indicator.text2 = "从远程拉取 $selectedBranch"
                        service.fetchRemoteBranch(repository, selectedBranch)

                        indicator.text = "变基中..."
                        indicator.text2 = "将 $currentBranch 变基到 $selectedBranch"
                        service.rebaseOnto(repository, selectedBranch)

                        indicator.text = "推送到远程..."
                        indicator.text2 = "强制推送 $currentBranch"
                        service.forcePushBranch(repository, currentBranch)

                        if (shouldCreateMR) {
                            indicator.text = "创建Merge请求..."
                            indicator.text2 = ""
                            createMergeRequest(currentBranch, commitMessage)
                        } else {
                            notifySuccess("变基并推送完成")
                        }

                    } catch (e: Exception) {
                        notifyError("操作失败: ${e.message}")
                    }
                }
            }
        )
    }

    private fun createMergeRequest(sourceBranch: String, description: String) {
        val mrService = project.service<MergeRequestService>()
        val result = mrService.createMergeRequest(
            repository,
            sourceBranch,
            selectedBranch,
            description
        )

        when (result) {
            is MergeRequestResult.Success -> {
                notifySuccess("变基推送完成，Merge请求已创建", result.url)
            }
            is MergeRequestResult.NotConfigured -> {
                notifySuccess("变基推送完成，但Merge请求未配置: ${result.reason}")
            }
            is MergeRequestResult.Error -> {
                notifySuccess("变基推送完成，但Merge请求创建失败: ${result.message}")
            }
        }
    }

    private fun notifySuccess(message: String, url: String? = null) {
        val fullMessage = if (url != null) {
            "$message\n<a href='$url'>查看Merge请求</a>"
        } else {
            message
        }
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Rebase Plugin")
            .createNotification(fullMessage, com.intellij.notification.NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyError(message: String) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Git Rebase Plugin")
            .createNotification(message, com.intellij.notification.NotificationType.ERROR)
            .notify(project)
    }
}

private class FuzzyBranchCompletionProvider(
    branches: Collection<String>
) : TextFieldWithAutoCompletionListProvider<String>(branches) {

    override fun getLookupString(item: String): String = item

    override fun compare(o1: String, o2: String): Int {
        return o1.compareTo(o2, ignoreCase = true)
    }

    override fun createPrefixMatcher(prefix: String): com.intellij.codeInsight.completion.PrefixMatcher {
        return FuzzyPrefixMatcher(prefix)
    }
}

private class FuzzyPrefixMatcher(prefix: String) : com.intellij.codeInsight.completion.PrefixMatcher(prefix) {

    private val matcher = com.intellij.psi.codeStyle.NameUtil.buildMatcher(
        prefix,
        com.intellij.psi.codeStyle.NameUtil.MatchingCaseSensitivity.NONE
    )

    override fun prefixMatches(name: String): Boolean {
        if (myPrefix.isEmpty()) return true
        if (name.startsWith(myPrefix, ignoreCase = true)) return true
        return matcher.matches(name)
    }

    override fun cloneWithPrefix(newPrefix: String): com.intellij.codeInsight.completion.PrefixMatcher {
        return if (newPrefix == myPrefix) this else FuzzyPrefixMatcher(newPrefix)
    }

    override fun isStartMatch(name: String): Boolean {
        return name.startsWith(myPrefix, ignoreCase = true) || matcher.isStartMatch(name)
    }

    override fun matchingDegree(name: String): Int {
        return when {
            name.equals(myPrefix, ignoreCase = true) -> Int.MAX_VALUE
            name.startsWith(myPrefix, ignoreCase = true) -> 1000
            else -> matcher.matchingDegree(name)
        }
    }
}