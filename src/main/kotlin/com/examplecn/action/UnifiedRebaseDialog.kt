package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
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
import com.intellij.util.ui.JBDimension
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class UnifiedRebaseDialog(
    private val project: Project,
    private val repository: GitRepository,
    private val initialCommitMessage: String? = null
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
        title = GitRebaseBundle.message("dialog.title")

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

        createMRCheckBox = JBCheckBox(GitRebaseBundle.message("option.create.mr"), false)

        appendTypeComboBox = JComboBox(arrayOf(
            GitRebaseBundle.message("commit.append.co.authored"),
            GitRebaseBundle.message("commit.append.jira"),
            GitRebaseBundle.message("commit.append.webhook"),
            GitRebaseBundle.message("commit.append.custom")
        ))
        appendInputField = JTextField(20)
        addAppendButton = JButton(GitRebaseBundle.message("commit.append.button"))

        appendTypeComboBox.addActionListener { updateAppendInputState() }
        appendInputField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateAppendButtonState()
        })
        addAppendButton.addActionListener { appendContent() }

        updateAppendInputState()

        // 如果从提交框架传入了初始提交信息，则预填充
        if (!initialCommitMessage.isNullOrBlank()) {
            messageArea.text = initialCommitMessage
        }

        init()

        // 设置OK按钮为默认样式（类似提交按钮）
        setOKButtonText(GitRebaseBundle.message("dialog.ok.button"))
        setCancelButtonText(GitRebaseBundle.message("dialog.cancel.button"))
    }

    override fun createActions(): Array<javax.swing.Action> {
        val actions = super.createActions()
        // 将OK按钮设置为默认按钮（获得焦点并使用蓝色高亮样式）
        myOKAction.putValue(javax.swing.Action.DEFAULT, true)
        return actions
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 0))
        mainPanel.preferredSize = Dimension(650, 480)
        mainPanel.border = JBUI.Borders.empty(12)

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // ===== 目标分支部分 =====
        contentPanel.add(TitledSeparator(GitRebaseBundle.message("dialog.section.target.branch")))
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
        contentPanel.add(TitledSeparator(GitRebaseBundle.message("dialog.section.changed.files")))
        contentPanel.add(Box.createVerticalStrut(8))

        val filesPanel = JPanel(BorderLayout(0, 8))
        filesPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val filesCountLabel = JBLabel()
        if (changedFiles.isEmpty()) {
            filesCountLabel.text = GitRebaseBundle.message("files.no.changes")
            filesCountLabel.foreground = UIUtil.getInactiveTextColor()
            filesCountLabel.icon = AllIcons.General.InspectionsOK
        } else {
            filesCountLabel.text = GitRebaseBundle.message("files.count", changedFiles.size)
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
            val warningText = JBLabel("<html>${GitRebaseBundle.message("files.warning")}</html>")
            warningText.foreground = JBUI.CurrentTheme.Banner.FOREGROUND

            warningPanel.add(warningIcon)
            warningPanel.add(warningText)

            filesListPanel.add(warningPanel, BorderLayout.SOUTH)
            filesPanel.add(filesListPanel, BorderLayout.CENTER)
        }

        contentPanel.add(filesPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 提交信息部分 =====
        contentPanel.add(TitledSeparator(GitRebaseBundle.message("dialog.section.commit.message")))
        contentPanel.add(Box.createVerticalStrut(8))

        val messagePanel = JPanel(BorderLayout())
        messagePanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.border = JBUI.Borders.empty(6, 8)
        messageArea.emptyText.text = GitRebaseBundle.message("commit.message.placeholder")
        val messageScroll = JBScrollPane(messageArea)
        messageScroll.preferredSize = Dimension(0, 90)
        messageScroll.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1)
        messagePanel.add(messageScroll, BorderLayout.CENTER)

        contentPanel.add(messagePanel)
        contentPanel.add(Box.createVerticalStrut(6))

        // 自动追加内容（紧跟在提交信息下方，作为辅助工具行）
        val appendPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        appendPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val appendLabel = JBLabel(GitRebaseBundle.message("commit.append.label"), AllIcons.General.Add, JBLabel.LEFT)
        appendLabel.foreground = UIUtil.getInactiveTextColor()
        appendPanel.add(appendLabel)
        appendPanel.add(appendTypeComboBox)
        appendPanel.add(appendInputField)
        appendPanel.add(addAppendButton)

        contentPanel.add(appendPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 其他选项 =====
        contentPanel.add(TitledSeparator(GitRebaseBundle.message("dialog.section.options")))
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
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("error.select.branch"),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        if (!branches.contains(selectedBranch)) {
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("error.branch.not.exist", selectedBranch),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        val currentBranch = ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.currentBranch?.name
            })

        if (selectedBranch == currentBranch) {
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("error.rebase.same.branch"),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        if (changedFiles.isNotEmpty() && messageArea.text.isBlank()) {
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("error.commit.message.required"),
                GitRebaseBundle.message("error.title")
            )
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

    private fun requiresInput(type: String): Boolean {
        return type == GitRebaseBundle.message("commit.append.jira") ||
               type == GitRebaseBundle.message("commit.append.custom")
    }

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
            GitRebaseBundle.message("commit.append.co.authored") -> "Co-Authored-By"
            GitRebaseBundle.message("commit.append.jira") -> {
                if (input.isEmpty()) return
                "\$(JIRA:$input)"
            }
            GitRebaseBundle.message("commit.append.webhook") -> "#webhook"
            GitRebaseBundle.message("commit.append.custom") -> {
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
            object : Task.Backgroundable(project, GitRebaseBundle.message("progress.title", selectedBranch), false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        if (changedFiles.isNotEmpty()) {
                            indicator.text = GitRebaseBundle.message("progress.commit.changes")
                            indicator.text2 = GitRebaseBundle.message("progress.commit.files", changedFiles.size)

                            service.addFiles(repository, changedFiles)
                            service.commitChanges(repository, commitMessage)
                        }

                        indicator.text = GitRebaseBundle.message("progress.fetch.remote")
                        indicator.text2 = GitRebaseBundle.message("progress.fetch.from.remote", selectedBranch)
                        service.fetchRemoteBranch(repository, selectedBranch)

                        indicator.text = GitRebaseBundle.message("progress.rebasing")
                        indicator.text2 = GitRebaseBundle.message("progress.rebase.onto", currentBranch, selectedBranch)
                        service.rebaseOnto(repository, selectedBranch)

                        indicator.text = GitRebaseBundle.message("progress.pushing")
                        indicator.text2 = GitRebaseBundle.message("progress.force.push", currentBranch)
                        service.forcePushBranch(repository, currentBranch)

                        if (shouldCreateMR) {
                            indicator.text = GitRebaseBundle.message("progress.creating.mr")
                            indicator.text2 = ""
                            createMergeRequest(currentBranch, commitMessage)
                        } else {
                            notifySuccess(GitRebaseBundle.message("notification.success"))
                        }

                    } catch (e: Exception) {
                        notifyError(GitRebaseBundle.message("error.operation.failed", e.message ?: "Unknown"))
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
                notifySuccess(GitRebaseBundle.message("notification.success.with.mr"), result.url)
            }

            is MergeRequestResult.NotConfigured -> {
                notifySuccess("${GitRebaseBundle.message("notification.success")}, ${result.reason}")
            }

            is MergeRequestResult.Error -> {
                notifySuccess("${GitRebaseBundle.message("notification.success")}, ${result.message}")
            }
        }
    }

    private fun notifySuccess(message: String, url: String? = null) {
        val fullMessage = if (url != null) {
            "$message\n<a href='$url'>${GitRebaseBundle.message("notification.view.mr")}</a>"
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