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
import com.intellij.icons.AllIcons
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import com.intellij.ui.components.*
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
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

    private val appendTypeComboBox: JComboBox<String>
    private val appendInputField: JTextField
    private val addAppendButton: JButton

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

        appendTypeComboBox = JComboBox(arrayOf("Co-Authored-By", "JIRA", "#webhook", "自定义内容"))
        appendInputField = JTextField(20)
        addAppendButton = JButton("添加")

        appendTypeComboBox.addActionListener { updateAppendInputState() }
        appendInputField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
        })
        addAppendButton.addActionListener { appendContent() }

        updateAppendInputState()

        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 0))
        mainPanel.preferredSize = Dimension(650, 480)
        mainPanel.border = JBUI.Borders.empty(12)

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // ===== 目标分支部分 =====
        contentPanel.add(TitledSeparator("目标分支"))
        contentPanel.add(Box.createVerticalStrut(8))

        val branchPanel = JPanel(BorderLayout(8, 0))
        branchPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val branchIcon = JBLabel(AllIcons.Vcs.Branch)
        branchPanel.add(branchIcon, BorderLayout.WEST)

        branchField.border = JBUI.Borders.empty(4)
        branchPanel.add(branchField, BorderLayout.CENTER)

        contentPanel.add(branchPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 变动文件部分 =====
        contentPanel.add(TitledSeparator("变动文件"))
        contentPanel.add(Box.createVerticalStrut(8))

        val filesPanel = JPanel(BorderLayout(0, 8))
        filesPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val filesCountLabel = JBLabel()
        if (changedFiles.isEmpty()) {
            filesCountLabel.text = "无变动文件"
            filesCountLabel.foreground = UIUtil.getInactiveTextColor()
            filesCountLabel.icon = AllIcons.General.InspectionsOK
        } else {
            filesCountLabel.text = "${changedFiles.size} 个文件将被提交"
            filesCountLabel.icon = AllIcons.Vcs.Changelist
        }
        filesPanel.add(filesCountLabel, BorderLayout.NORTH)

        if (changedFiles.isNotEmpty()) {
            val filesListPanel = JPanel(BorderLayout())
            filesListPanel.border = JBUI.Borders.empty(4, 0)

            val filesList = JBTextArea()
            filesList.isEditable = false
            filesList.background = UIUtil.getPanelBackground()
            filesList.foreground = UIUtil.getLabelForeground()
            filesList.font = UIUtil.getTreeFont()
            filesList.text = changedFiles.take(10).joinToString("\n") { "  • $it" } +
                            if (changedFiles.size > 10) "\n  ..." else ""
            filesList.border = JBUI.Borders.empty(4, 8)

            val filesScroll = JBScrollPane(filesList)
            filesScroll.preferredSize = Dimension(0, 80)
            filesScroll.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1)
            filesListPanel.add(filesScroll, BorderLayout.CENTER)

            val warningPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
            warningPanel.background = JBUI.CurrentTheme.Banner.WARNING_BACKGROUND
            warningPanel.border = JBUI.Borders.empty(6, 8)

            val warningIcon = JBLabel(AllIcons.General.Warning)
            val warningText = JBLabel("<html>所有变更文件将被自动提交（变基要求工作目录干净）</html>")
            warningText.foreground = JBUI.CurrentTheme.Banner.FOREGROUND

            warningPanel.add(warningIcon)
            warningPanel.add(warningText)

            filesListPanel.add(warningPanel, BorderLayout.SOUTH)
            filesPanel.add(filesListPanel, BorderLayout.CENTER)
        }

        contentPanel.add(filesPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 提交信息部分 =====
        contentPanel.add(TitledSeparator("提交信息"))
        contentPanel.add(Box.createVerticalStrut(8))

        val messagePanel = JPanel(BorderLayout())
        messagePanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.border = JBUI.Borders.empty(6, 8)
        messageArea.emptyText.text = "请输入提交信息…"
        val messageScroll = JBScrollPane(messageArea)
        messageScroll.preferredSize = Dimension(0, 90)
        messageScroll.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1)
        messagePanel.add(messageScroll, BorderLayout.CENTER)

        contentPanel.add(messagePanel)
        contentPanel.add(Box.createVerticalStrut(6))

        // 自动追加内容（紧跟在提交信息下方，作为辅助工具行）
        val appendPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        appendPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val appendLabel = JBLabel("追加:", AllIcons.General.Add, JBLabel.LEFT)
        appendLabel.foreground = UIUtil.getInactiveTextColor()
        appendPanel.add(appendLabel)
        appendPanel.add(appendTypeComboBox)
        appendPanel.add(appendInputField)
        appendPanel.add(addAppendButton)

        contentPanel.add(appendPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 其他选项 =====
        contentPanel.add(TitledSeparator("其他选项"))
        contentPanel.add(Box.createVerticalStrut(4))

        val mrPanel = JPanel(BorderLayout())
        mrPanel.border = JBUI.Borders.empty(0, 12, 0, 0)
        mrPanel.add(createMRCheckBox, BorderLayout.WEST)

        contentPanel.add(mrPanel)

        mainPanel.add(contentPanel, BorderLayout.CENTER)

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

        // 立即关闭对话框，在后台执行
        super.doOKAction()

        executeRebaseWorkflow(currentBranch ?: "")
    }

    override fun doCancelAction() {
        super.doCancelAction()
    }

    private fun requiresInput(type: String): Boolean = type == "JIRA" || type == "自定义内容"

    private fun updateAppendInputState() {
        val type = appendTypeComboBox.selectedItem as String
        appendInputField.isEnabled = requiresInput(type)
        if (!requiresInput(type)) {
            appendInputField.text = ""
        }
        updateAppendButtonState()
    }

    private fun updateAppendButtonState() {
        val type = appendTypeComboBox.selectedItem as String
        addAppendButton.isEnabled = !requiresInput(type) || appendInputField.text.isNotBlank()
    }

    private fun appendContent() {
        val type = appendTypeComboBox.selectedItem as String
        val input = appendInputField.text.trim()

        val tag = when (type) {
            "Co-Authored-By" -> "Co-Authored-By"
            "JIRA" -> {
                if (input.isEmpty()) return
                "\$(JIRA:$input)"
            }
            "#webhook" -> "#webhook"
            "自定义内容" -> {
                if (input.isEmpty()) return
                input
            }
            else -> return
        }

        val current = messageArea.text
        val separator = if (current.isNotEmpty() && !current.endsWith(";") && !current.endsWith("\n")) ";" else ""
        messageArea.text = current + separator + tag

        appendInputField.text = ""
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