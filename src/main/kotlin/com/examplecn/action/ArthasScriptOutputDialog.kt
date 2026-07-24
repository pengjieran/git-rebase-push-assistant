package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.*
import kotlin.io.path.writeText

/**
 * Dialog to display and save the generated Arthas hotfix script
 */
class ArthasScriptOutputDialog(
    private val project: Project,
    private val className: String,
    private val scriptContent: String,
    private val clipboardContent: String,
    private val classFile: File
) : DialogWrapper(project) {

    private val scriptTextArea = JTextArea()

    init {
        title = GitRebaseBundle.message("arthas.output.dialog.title")
        init()
        setupUI()
    }

    private fun setupUI() {
        scriptTextArea.text = scriptContent
        scriptTextArea.isEditable = false
        scriptTextArea.lineWrap = false
        scriptTextArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.preferredSize = Dimension(700, 400)

        // Top panel with file info
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)

        // Class name info
        val infoLabel = JBLabel(GitRebaseBundle.message("arthas.output.dialog.info", className))
        topPanel.add(infoLabel)

        // File path
        val pathLabel = JBLabel(GitRebaseBundle.message("arthas.output.file.path") + " " + classFile.absolutePath)
        pathLabel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        topPanel.add(pathLabel)

        // Last modified time
        val modifiedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(classFile.lastModified()))
        val modifiedLabel = JBLabel(GitRebaseBundle.message("arthas.output.file.modified") + " " + modifiedTime)
        modifiedLabel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        topPanel.add(modifiedLabel)

        panel.add(topPanel, BorderLayout.NORTH)

        // Script content
        val scrollPane = JBScrollPane(scriptTextArea)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    override fun createActions(): Array<Action> {
        val copyAction = object : AbstractAction(GitRebaseBundle.message("arthas.output.copy.button")) {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                CopyPasteManager.getInstance().setContents(StringSelection(clipboardContent))
                Messages.showInfoMessage(
                    project,
                    GitRebaseBundle.message("arthas.output.copied"),
                    GitRebaseBundle.message("arthas.dialog.title")
                )
            }
        }

        val saveAction = object : AbstractAction(GitRebaseBundle.message("arthas.output.save.button")) {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                saveScript()
            }
        }

        val closeAction = object : AbstractAction(GitRebaseBundle.message("arthas.output.close.button")) {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                close(OK_EXIT_CODE)
            }
        }

        return arrayOf(copyAction, saveAction, closeAction)
    }

    private fun saveScript() {
        val descriptor = FileSaverDescriptor(
            GitRebaseBundle.message("arthas.output.save.title"),
            GitRebaseBundle.message("arthas.output.save.description"),
            "sh"
        )

        val saveDialog = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)

        val timestamp = System.currentTimeMillis()
        val defaultFileName = "${className}_hotfix_$timestamp.sh"

        // Get base directory as VirtualFile
        val baseDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            .findFileByPath(project.basePath ?: System.getProperty("user.home"))

        val fileWrapper = saveDialog.save(baseDir, defaultFileName)
        if (fileWrapper != null) {
            try {
                // VirtualFileWrapper.getFile() returns Path
                val path = fileWrapper.file
                path.writeText(scriptContent)

                // Make script executable on Unix-like systems
                val file = File(path.toString())
                val osName = System.getProperty("os.name").lowercase()
                if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
                    file.setExecutable(true)
                }

                Messages.showInfoMessage(
                    project,
                    GitRebaseBundle.message("arthas.output.saved", path.toString()),
                    GitRebaseBundle.message("arthas.dialog.title")
                )
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    GitRebaseBundle.message("arthas.error.save.failed", e.message ?: "Unknown error"),
                    GitRebaseBundle.message("error.title")
                )
            }
        }
    }
}