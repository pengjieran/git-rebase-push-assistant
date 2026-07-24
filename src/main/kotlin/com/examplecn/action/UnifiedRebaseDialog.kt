package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.examplecn.config.GitRebaseSettings
import com.examplecn.service.GitRebaseService
import com.examplecn.service.MergeRequestResult
import com.examplecn.service.MergeRequestService
import com.examplecn.service.OpenAIService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.icons.AllIcons
import com.intellij.ui.components.*
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.JBDimension
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class UnifiedRebaseDialog(
    private val project: Project,
    private val repository: GitRepository,
    private val initialCommitMessage: String? = null,
    private val preloadedChangedFiles: List<String>? = null,
    private val preloadedBranches: List<String>? = null
) : DialogWrapper(project) {

    private val service = project.service<GitRebaseService>()
    private val branches: List<String>
    private val changedFiles: List<String>

    private val branchComboBox: ComboBox<String>
    private val messageArea = JBTextArea(4, 50)
    private val enableRebaseCheckBox: JBCheckBox
    private val createMRCheckBox: JBCheckBox
    private val aiGenerateButton: JButton

    private val appendTypeComboBox: ComboBox<String>
    private val appendInputField: JTextField
    private val addAppendButton: JButton

    var selectedBranch: String = ""
        private set
    var commitMessage: String = ""
        private set
    var shouldCreateMR: Boolean = false
        private set
    var shouldRebase: Boolean = true
        private set

    init {
        title = GitRebaseBundle.message("dialog.title")

        // Use preloaded data if provided (to avoid EDT blocking), otherwise load them
        branches = preloadedBranches ?: service.getRemoteBranches(repository)
        changedFiles = preloadedChangedFiles ?: emptyList()

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

        // 创建可编辑的下拉框，支持直接输入和模糊搜索
        branchComboBox = ComboBox(branches.toTypedArray())
        branchComboBox.isEditable = true
        branchComboBox.selectedItem = suggestedBranch ?: ""

        // 添加自定义过滤器实现模糊搜索
        val editor = branchComboBox.editor.editorComponent as? JTextField
        editor?.let { textField ->
            textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
                override fun insertUpdate(e: javax.swing.event.DocumentEvent) = filterBranches()
                override fun removeUpdate(e: javax.swing.event.DocumentEvent) = filterBranches()
                override fun changedUpdate(e: javax.swing.event.DocumentEvent) = filterBranches()

                private fun filterBranches() {
                    if (branchComboBox.isPopupVisible) {
                        val input = textField.text.lowercase()
                        if (input.isEmpty()) {
                            updateComboBoxItems(branches)
                        } else {
                            val filtered = branches.filter {
                                it.lowercase().contains(input) ||
                                fuzzyMatch(it.lowercase(), input)
                            }
                            updateComboBoxItems(filtered)
                        }
                    }
                }

                private fun fuzzyMatch(text: String, pattern: String): Boolean {
                    var textIndex = 0
                    for (char in pattern) {
                        textIndex = text.indexOf(char, textIndex)
                        if (textIndex == -1) return false
                        textIndex++
                    }
                    return true
                }

                private fun updateComboBoxItems(items: List<String>) {
                    val currentText = textField.text
                    branchComboBox.removeAllItems()
                    items.forEach { branchComboBox.addItem(it) }
                    textField.text = currentText
                }
            })
        }

        // 防止选中时覆盖用户输入
        branchComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED && branchComboBox.isPopupVisible) {
                editor?.text = e.item as? String ?: ""
            }
        }

        val enableRebaseDefault = GitRebaseSettings.getInstance(project).state.enableRebase
        enableRebaseCheckBox = JBCheckBox(GitRebaseBundle.message("option.enable.rebase"), enableRebaseDefault)

        createMRCheckBox = JBCheckBox(GitRebaseBundle.message("option.create.mr"), true)

        // 仅当变基和提交MR都未选中时，禁用目标分支选择
        val branchEnableListener = { updateBranchSelectorState() }
        enableRebaseCheckBox.addItemListener { branchEnableListener() }
        createMRCheckBox.addItemListener { branchEnableListener() }

        aiGenerateButton = JButton(GitRebaseBundle.message("openai.generate.button"))
        aiGenerateButton.icon = AllIcons.Actions.IntentionBulb
        aiGenerateButton.addActionListener { generateCommitMessageWithAI() }

        appendTypeComboBox = ComboBox(arrayOf(
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
        updateBranchSelectorState()

        // 如果从提交框架传入了初始提交信息，则预填充
        if (!initialCommitMessage.isNullOrBlank()) {
            messageArea.text = initialCommitMessage
        }

        init()

        // 设置OK按钮为默认样式（类似提交按钮）
        setOKButtonText(GitRebaseBundle.message("dialog.ok.button"))
        setCancelButtonText(GitRebaseBundle.message("dialog.cancel.button"))
    }

    override fun createActions(): Array<Action> {
        val actions = super.createActions()
        // 将OK按钮设置为默认按钮（获得焦点并使用蓝色高亮样式）
        myOKAction.putValue(Action.DEFAULT, true)
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
        branchPanel.border = JBUI.Borders.emptyLeft(12)

        val branchIcon = JBLabel(AllIcons.Vcs.Branch)
        branchPanel.add(branchIcon, BorderLayout.WEST)

        branchComboBox.preferredSize = Dimension(400, branchComboBox.preferredSize.height)
        branchPanel.add(branchComboBox, BorderLayout.CENTER)

        contentPanel.add(branchPanel)
        contentPanel.add(Box.createVerticalStrut(16))

        // ===== 变动文件部分 =====
        contentPanel.add(TitledSeparator(GitRebaseBundle.message("dialog.section.changed.files")))
        contentPanel.add(Box.createVerticalStrut(8))

        val filesPanel = JPanel(BorderLayout(0, 8))
        filesPanel.border = JBUI.Borders.emptyLeft(12)

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

        // 顶部按钮栏（AI生成按钮）
        val messageTopPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        messageTopPanel.add(aiGenerateButton)
        messagePanel.add(messageTopPanel, BorderLayout.NORTH)

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

        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.border = JBUI.Borders.empty(0, 12, 0, 0)

        val rebaseRow = JPanel(BorderLayout())
        rebaseRow.add(enableRebaseCheckBox, BorderLayout.WEST)
        optionsPanel.add(rebaseRow)

        val mrRow = JPanel(BorderLayout())
        mrRow.add(createMRCheckBox, BorderLayout.WEST)
        optionsPanel.add(mrRow)

        contentPanel.add(optionsPanel)

        mainPanel.add(contentPanel, BorderLayout.CENTER)

        return mainPanel
    }

    override fun doOKAction() {
        shouldRebase = enableRebaseCheckBox.isSelected
        shouldCreateMR = createMRCheckBox.isSelected

        val editor = branchComboBox.editor.editorComponent as? JTextField
        selectedBranch = (editor?.text ?: branchComboBox.selectedItem as? String ?: "").trim()

        val currentBranch = ApplicationManager.getApplication()
            .runReadAction(com.intellij.openapi.util.Computable {
                repository.currentBranch?.name
            })

        // 目标分支仅在变基或创建MR时才需要，此时才校验
        val branchNeeded = shouldRebase || shouldCreateMR
        if (branchNeeded) {
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

            // 变基不能以当前分支为基准；仅创建MR时允许目标分支等于当前分支的场景交由后续处理
            if (shouldRebase && selectedBranch == currentBranch) {
                Messages.showErrorDialog(
                    project,
                    GitRebaseBundle.message("error.rebase.same.branch"),
                    GitRebaseBundle.message("error.title")
                )
                return
            }
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

        // 持久化变基偏好
        GitRebaseSettings.getInstance(project).state.enableRebase = shouldRebase

        // 立即关闭对话框，在后台执行
        super.doOKAction()

        executeRebaseWorkflow(currentBranch ?: "")
    }

    override fun doCancelAction() {
        super.doCancelAction()
    }

    /**
     * 目标分支仅在需要时可用：变基需要目标分支作为变基基准，
     * 创建MR需要目标分支作为合并目标。两者都未选中时禁用选择器。
     */
    private fun updateBranchSelectorState() {
        val branchNeeded = enableRebaseCheckBox.isSelected || createMRCheckBox.isSelected
        branchComboBox.isEnabled = branchNeeded
        (branchComboBox.editor.editorComponent as? JTextField)?.isEnabled = branchNeeded
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

                        if (shouldRebase) {
                            indicator.text = GitRebaseBundle.message("progress.fetch.remote")
                            indicator.text2 = GitRebaseBundle.message("progress.fetch.from.remote", selectedBranch)
                            service.fetchRemoteBranch(repository, selectedBranch)

                            indicator.text = GitRebaseBundle.message("progress.rebasing")
                            indicator.text2 = GitRebaseBundle.message("progress.rebase.onto", currentBranch, selectedBranch)
                            service.rebaseOnto(repository, selectedBranch)

                            indicator.text = GitRebaseBundle.message("progress.pushing")
                            indicator.text2 = GitRebaseBundle.message("progress.force.push", currentBranch)
                            service.forcePushBranch(repository, currentBranch)
                        } else {
                            // 未变基：使用普通 push，避免误覆盖远程历史
                            indicator.text = GitRebaseBundle.message("progress.pushing")
                            indicator.text2 = GitRebaseBundle.message("progress.normal.push", currentBranch)
                            service.pushBranch(repository, currentBranch)
                        }

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

    private fun generateCommitMessageWithAI() {
        if (changedFiles.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "没有变更文件，无需生成提交信息",
                "AI生成"
            )
            return
        }

        val settings = GitRebaseSettings.getInstance(project).state
        if (settings.openaiApiKey.isBlank()) {
            val result = Messages.showYesNoDialog(
                project,
                GitRebaseBundle.message("openai.error.not.configured"),
                "AI生成",
                "去配置",
                "取消",
                Messages.getWarningIcon()
            )
            if (result == Messages.YES) {
                // 打开设置页面
                com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project, GitRebaseBundle.message("settings.displayName"))
            }
            return
        }

        // 禁用编辑和按钮，显示生成状态
        messageArea.isEditable = false
        aiGenerateButton.isEnabled = false
        aiGenerateButton.text = GitRebaseBundle.message("openai.generating")

        // 使用 ProgressManager 显示后台任务状态
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "AI生成提交信息", false) {
                var generatedMessage: String? = null
                var errorMessage: String? = null

                override fun run(indicator: ProgressIndicator) {
                    try {
                        indicator.text = "正在获取代码变更..."
                        val diff = service.getDiff(repository)

                        indicator.text = "正在调用OpenAI API..."
                        indicator.text2 = "生成中，请稍候"

                        val openaiService = OpenAIService.getInstance(project)
                        val result = openaiService.generateCommitMessage(changedFiles, diff)

                        if (result.isSuccess) {
                            generatedMessage = result.getOrNull()
                            indicator.text = "生成成功"
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Unknown"
                            indicator.text = "生成失败"
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Unknown"
                    }
                }

                override fun onSuccess() {
                    // 恢复UI状态
                    messageArea.isEditable = true
                    aiGenerateButton.isEnabled = true
                    aiGenerateButton.text = GitRebaseBundle.message("openai.generate.button")

                    if (generatedMessage != null) {
                        // 成功：直接替换提交信息
                        messageArea.text = generatedMessage
                        messageArea.caretPosition = 0

                        // 显示成功通知（简短提示）
                        com.intellij.notification.NotificationGroupManager.getInstance()
                            .getNotificationGroup("Git Rebase Plugin")
                            .createNotification(
                                "提交信息已生成",
                                com.intellij.notification.NotificationType.INFORMATION
                            )
                            .notify(project)
                    } else if (errorMessage != null) {
                        // 失败：显示详细错误
                        if (errorMessage!!.length > 200 || errorMessage!!.contains("\n")) {
                            showDetailedErrorDialog(errorMessage!!)
                        } else {
                            Messages.showErrorDialog(
                                project,
                                GitRebaseBundle.message("openai.error.failed", errorMessage!!),
                                "AI生成失败"
                            )
                        }
                    }
                }

                override fun onThrowable(error: Throwable) {
                    // 恢复UI状态
                    messageArea.isEditable = true
                    aiGenerateButton.isEnabled = true
                    aiGenerateButton.text = GitRebaseBundle.message("openai.generate.button")

                    val msg = error.message ?: "Unknown"
                    if (msg.length > 200 || msg.contains("\n")) {
                        showDetailedErrorDialog(msg)
                    } else {
                        Messages.showErrorDialog(
                            project,
                            GitRebaseBundle.message("openai.error.failed", msg),
                            "AI生成失败"
                        )
                    }
                }
            }
        )
    }

    private fun showDetailedErrorDialog(errorMessage: String) {
        val dialog = object : DialogWrapper(project) {
            init {
                title = "AI生成失败"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout())
                panel.preferredSize = JBDimension(600, 300)

                val textArea = JBTextArea(errorMessage)
                textArea.isEditable = false
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                textArea.caretPosition = 0

                val scrollPane = JBScrollPane(textArea)
                panel.add(scrollPane, BorderLayout.CENTER)

                return panel
            }

            override fun createActions(): Array<javax.swing.Action> {
                return arrayOf(okAction)
            }
        }
        dialog.show()
    }
}