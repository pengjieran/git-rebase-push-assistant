package com.examplecn.config

import com.examplecn.service.OpenAIService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Git Rebase插件配置页面
 */
class GitRebaseSettingsConfigurable(private val project: Project) : Configurable {

    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val testButton = JButton("测试连接")

    init {
        testButton.addActionListener {
            testConnection()
        }
    }

    override fun getDisplayName(): String = "Git Rebase & Push"

    override fun createComponent(): JComponent {
        val panel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><b>OpenAI配置</b></html>"), 0)
            .addVerticalGap(8)
            .addLabeledComponent("Base URL:", baseUrlField, 1, false)
            .addComponentToRightColumn(JBLabel("<html><font color='gray'>示例: https://api.openai.com/v1</font></html>"), 0)
            .addVerticalGap(4)
            .addLabeledComponent("模型ID:", modelField, 1, false)
            .addComponentToRightColumn(JBLabel("<html><font color='gray'>示例: gpt-4o-mini, gpt-4, gpt-3.5-turbo</font></html>"), 0)
            .addVerticalGap(4)
            .addLabeledComponent("API Key:", apiKeyField, 1, false)
            .addComponentToRightColumn(JBLabel("<html><font color='gray'>用于生成提交信息的OpenAI API密钥</font></html>"), 0)
            .addVerticalGap(8)
            .addComponent(testButton, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel.border = JBUI.Borders.empty(10)
        return panel
    }

    override fun isModified(): Boolean {
        val settings = GitRebaseSettings.getInstance(project).state
        return baseUrlField.text != settings.openaiBaseUrl ||
                modelField.text != settings.openaiModel ||
                String(apiKeyField.password) != settings.openaiApiKey
    }

    override fun apply() {
        val settings = GitRebaseSettings.getInstance(project)
        settings.state.openaiBaseUrl = baseUrlField.text.trim()
        settings.state.openaiModel = modelField.text.trim()
        settings.state.openaiApiKey = String(apiKeyField.password).trim()
    }

    override fun reset() {
        val settings = GitRebaseSettings.getInstance(project).state
        baseUrlField.text = settings.openaiBaseUrl
        modelField.text = settings.openaiModel
        apiKeyField.text = settings.openaiApiKey
    }

    private fun testConnection() {
        val baseUrl = baseUrlField.text.trim()
        val model = modelField.text.trim()
        val apiKey = String(apiKeyField.password).trim()

        if (apiKey.isBlank()) {
            Messages.showErrorDialog(project, "请先输入API Key", "测试连接")
            return
        }

        testButton.isEnabled = false
        testButton.text = "测试中..."

        Thread {
            try {
                val service = OpenAIService.getInstance(project)
                val result = service.testConnection(baseUrl, apiKey, model)

                javax.swing.SwingUtilities.invokeLater {
                    testButton.isEnabled = true
                    testButton.text = "测试连接"

                    if (result.isSuccess) {
                        Messages.showInfoMessage(
                            project,
                            "连接成功！\n响应: ${result.getOrNull()}",
                            "测试连接"
                        )
                    } else {
                        Messages.showErrorDialog(
                            project,
                            "连接失败：\n${result.exceptionOrNull()?.message}",
                            "测试连接"
                        )
                    }
                }
            } catch (e: Exception) {
                javax.swing.SwingUtilities.invokeLater {
                    testButton.isEnabled = true
                    testButton.text = "测试连接"
                    Messages.showErrorDialog(
                        project,
                        "测试失败：\n${e.message}",
                        "测试连接"
                    )
                }
            }
        }.start()
    }
}