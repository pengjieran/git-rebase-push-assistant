package com.examplecn

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.random.Random

/**
 * 示例ToolWindow
 * 这是IntelliJ Platform Plugin Template自带的示例，展示如何创建Tool Window
 */
class GitPluginWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private class MyToolWindow(private val toolWindow: ToolWindow) {
        private val numberLabel = JBLabel("The random number is: 0")

        val content: JPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(numberLabel)
            add(JButton("Shuffle").apply {
                addActionListener {
                    numberLabel.text = "The random number is: ${Random.nextInt(0, 100)}"
                }
            })
        }
    }
}