package com.examplecn.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class CommitFilesDialog(
    private val project: Project,
    private val changedFiles: List<String>
) : DialogWrapper(project) {

    private val fileList = CheckBoxList<String>()
    private val messageArea = JBTextArea(4, 40)

    var selectedFiles: List<String> = emptyList()
        private set
    var commitMessage: String = ""
        private set

    init {
        title = "选择要提交的变动文件"

        fileList.setItems(changedFiles) { it }
        for (index in changedFiles.indices) {
            fileList.setItemSelected(changedFiles[index], true)
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 12))
        panel.preferredSize = Dimension(520, 440)
        panel.border = JBUI.Borders.empty(8, 4)

        val filesPanel = JPanel(BorderLayout(0, 8))
        filesPanel.add(buildFilesHeader(), BorderLayout.NORTH)
        filesPanel.add(buildWarningBanner(), BorderLayout.CENTER)

        val listWrapper = JPanel(BorderLayout())
        listWrapper.border = BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        listWrapper.add(JBScrollPane(fileList), BorderLayout.CENTER)

        val filesSection = JPanel(BorderLayout(0, 8))
        filesSection.add(filesPanel, BorderLayout.NORTH)
        filesSection.add(listWrapper, BorderLayout.CENTER)

        val messagePanel = JPanel(BorderLayout(0, 6))
        val messageLabel = JBLabel("提交信息")
        messageLabel.font = messageLabel.font.deriveFont(java.awt.Font.BOLD)
        messagePanel.add(messageLabel, BorderLayout.NORTH)
        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.border = JBUI.Borders.empty(4)
        messagePanel.add(JBScrollPane(messageArea), BorderLayout.CENTER)
        messagePanel.border = JBUI.Borders.emptyTop(4)

        panel.add(filesSection, BorderLayout.CENTER)
        panel.add(messagePanel, BorderLayout.SOUTH)

        return panel
    }

    private fun buildFilesHeader(): JComponent {
        val label = JBLabel("变动文件")
        label.font = label.font.deriveFont(java.awt.Font.BOLD)
        val hint = JBLabel("勾选要提交的文件")
        hint.foreground = JBUI.CurrentTheme.Label.disabledForeground()
        val header = JPanel(BorderLayout(6, 0))
        header.add(label, BorderLayout.WEST)
        header.add(hint, BorderLayout.CENTER)
        return header
    }

    private fun buildWarningBanner(): JComponent {
        val banner = JBLabel(
            "<html><body style='width: 460px'>" +
                    "⚠ 仅支持<b>全部提交</b>或<b>取消操作</b>，未勾选的文件不会被暂存，可能导致变基失败" +
                    "</body></html>"
        )
        banner.horizontalAlignment = SwingConstants.LEFT
        banner.foreground = java.awt.Color(0xB3, 0x66, 0x00)
        banner.isOpaque = true
        banner.background = java.awt.Color(0xFF, 0xF4, 0xE0)
        banner.border = JBUI.Borders.empty(6, 8)
        return banner
    }

    override fun doOKAction() {
        selectedFiles = fileList.checkedItems

        if (selectedFiles.isNotEmpty() && messageArea.text.isBlank()) {
            Messages.showErrorDialog(project, "请填写提交信息", "错误")
            return
        }

        commitMessage = messageArea.text.trim()
        super.doOKAction()
    }
}