package com.examplecn.action

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * Co-Authored-By 选择对话框
 * 支持从预定义列表中选择多个AI助手，或自定义输入
 */
class CoAuthoredByDialog(private val project: Project) : DialogWrapper(project) {

    private val selectedAuthors = mutableSetOf<AuthorInfo>()
    private val customNameField = JBTextField(30)
    private val customEmailField = JBTextField(30)
    private val selectedListModel = DefaultListModel<String>()
    private val selectedList = JBList(selectedListModel)

    // 预定义的AI助手列表
    private val predefinedAuthors = listOf(
        AuthorInfo("Claude Code", "claude-code@anthropic.com"),
        AuthorInfo("Cursor", "cursor@cursor.sh"),
        AuthorInfo("Yonwork", "Yonwork@yonyou.com"),
        AuthorInfo("OPENAI", "openai@openai.com"),
        AuthorInfo("GitHub Copilot", "copilot@github.com"),
        AuthorInfo("CodeBuddy", "codebuddy@ai-assistant.com"),
        AuthorInfo("Lingma", "lingma@alibaba.com"),
        AuthorInfo("Trae", "trae@trae.ai"),
        AuthorInfo("DeepSeek", "deepseek@deepseek.ai"),
        AuthorInfo("GLM", "glm@glm.com")
    )

    init {
        title = "选择 Co-Authored-By"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.preferredSize = Dimension(600, 450)
        mainPanel.border = JBUI.Borders.empty(10)

        // 顶部说明
        val instructionPanel = JPanel(BorderLayout())
        instructionPanel.border = JBUI.Borders.empty(0, 0, 10, 0)
        val instructionText = JBLabel(
            "<html><b>使用说明：</b>从左侧选择AI助手（可按住Ctrl/Cmd多选），点击\"添加选中 →\"按钮，或在底部自定义输入作者信息。</html>"
        )
        instructionText.foreground = com.intellij.util.ui.UIUtil.getContextHelpForeground()
        instructionPanel.add(instructionText, BorderLayout.CENTER)
        mainPanel.add(instructionPanel, BorderLayout.NORTH)

        // 中间内容面板
        val contentPanel = JPanel(BorderLayout())

        // 左侧：预定义列表
        val leftPanel = JPanel(BorderLayout())
        leftPanel.border = JBUI.Borders.empty(0, 0, 0, 5)

        val predefinedLabel = JBLabel("📋 预定义AI助手（可多选）：")
        leftPanel.add(predefinedLabel, BorderLayout.NORTH)

        val predefinedListModel = DefaultListModel<String>()
        predefinedAuthors.forEach { predefinedListModel.addElement(it.displayName) }

        val predefinedList = JBList(predefinedListModel)
        predefinedList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        predefinedList.emptyText.text = "暂无可选项"
        val predefinedScroll = JBScrollPane(predefinedList)
        predefinedScroll.preferredSize = Dimension(250, 0)
        leftPanel.add(predefinedScroll, BorderLayout.CENTER)

        val addPredefinedButton = JButton("➡️ 添加选中")
        addPredefinedButton.toolTipText = "将选中的AI助手添加到右侧列表（可按住Ctrl/Cmd多选）"
        addPredefinedButton.addActionListener {
            if (predefinedList.selectedValuesList.isEmpty()) {
                JOptionPane.showMessageDialog(
                    contentPane,
                    "请先在左侧列表中选择至少一个AI助手",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return@addActionListener
            }
            predefinedList.selectedValuesList.forEach { displayName ->
                predefinedAuthors.find { it.displayName == displayName }?.let { author ->
                    if (selectedAuthors.add(author)) {
                        selectedListModel.addElement(author.formatCoAuthoredBy())
                    }
                }
            }
            predefinedList.clearSelection()
        }
        val addButtonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        addButtonPanel.add(addPredefinedButton)
        leftPanel.add(addButtonPanel, BorderLayout.SOUTH)

        // 右侧：已选择列表 + 自定义输入
        val rightPanel = JPanel(BorderLayout(0, 10))
        rightPanel.border = JBUI.Borders.empty(0, 5, 0, 0)

        // 已选择列表
        val selectedLabel = JBLabel("✅ 已选择（将追加到提交信息）：")
        rightPanel.add(selectedLabel, BorderLayout.NORTH)

        selectedList.emptyText.text = "尚未选择任何作者"
        val selectedScroll = JBScrollPane(selectedList)
        selectedScroll.preferredSize = Dimension(250, 200)
        rightPanel.add(selectedScroll, BorderLayout.CENTER)

        val removeButton = JButton("❌ 移除选中")
        removeButton.toolTipText = "从已选择列表中移除选中的项"
        removeButton.addActionListener {
            if (selectedList.selectedValuesList.isEmpty()) {
                JOptionPane.showMessageDialog(
                    contentPane,
                    "请先在右侧列表中选择要移除的项",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return@addActionListener
            }
            selectedList.selectedValuesList.forEach { formatted ->
                selectedAuthors.removeIf { it.formatCoAuthoredBy() == formatted }
                selectedListModel.removeElement(formatted)
            }
        }
        val removeButtonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        removeButtonPanel.add(removeButton)
        rightPanel.add(removeButtonPanel, BorderLayout.SOUTH)

        // 底部：自定义输入区域
        val customPanel = JPanel()
        customPanel.layout = BoxLayout(customPanel, BoxLayout.Y_AXIS)
        customPanel.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(com.intellij.util.ui.UIUtil.getBoundsColor(), 1, 0, 0, 0),
            JBUI.Borders.empty(10, 0, 0, 0)
        )

        val customLabel = JBLabel("✏️ 自定义输入（用于添加团队成员或其他协作者）：")
        customPanel.add(customLabel)
        customPanel.add(Box.createVerticalStrut(8))

        val namePanel = JPanel(BorderLayout(5, 0))
        namePanel.add(JBLabel("名称："), BorderLayout.WEST)
        customNameField.emptyText.text = "例如：Team Lead"
        namePanel.add(customNameField, BorderLayout.CENTER)
        customPanel.add(namePanel)
        customPanel.add(Box.createVerticalStrut(5))

        val emailPanel = JPanel(BorderLayout(5, 0))
        emailPanel.add(JBLabel("邮箱："), BorderLayout.WEST)
        customEmailField.emptyText.text = "例如：lead@company.com"
        emailPanel.add(customEmailField, BorderLayout.CENTER)
        customPanel.add(emailPanel)
        customPanel.add(Box.createVerticalStrut(8))

        val addCustomButton = JButton("➕ 添加自定义")
        addCustomButton.toolTipText = "添加自定义的作者到右侧列表"
        addCustomButton.addActionListener {
            val name = customNameField.text.trim()
            val email = customEmailField.text.trim()

            if (name.isNotEmpty() && email.isNotEmpty()) {
                val author = AuthorInfo(name, email)
                if (selectedAuthors.add(author)) {
                    selectedListModel.addElement(author.formatCoAuthoredBy())
                    customNameField.text = ""
                    customEmailField.text = ""
                }
            } else {
                JOptionPane.showMessageDialog(
                    contentPane,
                    "请输入名称和邮箱",
                    "输入错误",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        val addCustomButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        addCustomButtonPanel.add(addCustomButton)
        customPanel.add(addCustomButtonPanel)

        // 组装布局
        val topPanel = JPanel(BorderLayout())
        topPanel.add(leftPanel, BorderLayout.WEST)
        topPanel.add(rightPanel, BorderLayout.CENTER)

        contentPanel.add(topPanel, BorderLayout.CENTER)
        mainPanel.add(contentPanel, BorderLayout.CENTER)
        mainPanel.add(customPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    override fun doOKAction() {
        if (selectedAuthors.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPane,
                "请至少选择一个作者",
                "未选择",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        super.doOKAction()
    }

    /**
     * 获取选择的作者，用分号分隔
     */
    fun getFormattedCoAuthoredBy(): String {
        return selectedAuthors.joinToString(";") { it.formatCoAuthoredBy() }
    }

    /**
     * 作者信息数据类
     */
    data class AuthorInfo(val name: String, val email: String) {
        val displayName: String
            get() = name

        fun formatCoAuthoredBy(): String = "Co-Authored-By: $name <$email>"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AuthorInfo) return false
            return email == other.email
        }

        override fun hashCode(): Int = email.hashCode()
    }
}
