package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Dialog for selecting .class files to generate Arthas hotfix scripts
 */
class ArthasHotfixDialog(private val project: Project) : DialogWrapper(project) {

    private val directoryField = TextFieldWithBrowseButton()
    private val tableModel = DefaultTableModel(arrayOf("Class Name", "Path", "Size"), 0)
    private val classTable = JBTable(tableModel)
    var selectedClassFile: File? = null
        private set

    init {
        title = GitRebaseBundle.message("arthas.dialog.title")
        init()
        setupUI()
        loadDefaultDirectories()
        updateOKActionState()
    }

    private fun setupUI() {
        // Setup directory chooser
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withTitle(GitRebaseBundle.message("arthas.dialog.select.directory"))
            .withDescription(GitRebaseBundle.message("arthas.dialog.select.directory.description"))

        directoryField.addBrowseFolderListener(
            GitRebaseBundle.message("arthas.dialog.select.directory"),
            null,
            project,
            descriptor
        )

        directoryField.addActionListener {
            val dir = directoryField.text
            if (dir.isNotBlank()) {
                scanDirectory(File(dir))
            }
        }

        // Setup table
        classTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        classTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                if (classTable.selectedRow >= 0) {
                    val selectedPath = tableModel.getValueAt(classTable.selectedRow, 1) as String
                    selectedClassFile = File(selectedPath)
                } else {
                    selectedClassFile = null
                }
                updateOKActionState()
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.preferredSize = Dimension(800, 500)

        // Top section: directory selector
        val topPanel = JPanel(BorderLayout(5, 0))
        topPanel.add(JBLabel(GitRebaseBundle.message("arthas.dialog.output.directory") + ":"), BorderLayout.WEST)
        topPanel.add(directoryField, BorderLayout.CENTER)

        val scanButton = JButton(GitRebaseBundle.message("arthas.dialog.scan.button"))
        scanButton.addActionListener {
            val dir = directoryField.text
            if (dir.isNotBlank()) {
                scanDirectory(File(dir))
            } else {
                Messages.showWarningDialog(
                    project,
                    GitRebaseBundle.message("arthas.error.no.directory"),
                    GitRebaseBundle.message("error.title")
                )
            }
        }
        topPanel.add(scanButton, BorderLayout.EAST)

        panel.add(topPanel, BorderLayout.NORTH)

        // Center section: table of .class files
        val scrollPane = JBScrollPane(classTable)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Bottom section: info label
        val infoLabel = JBLabel(GitRebaseBundle.message("arthas.dialog.info"))
        panel.add(infoLabel, BorderLayout.SOUTH)

        return panel
    }

    private fun loadDefaultDirectories() {
        val basePath = project.basePath ?: return

        // Try common output directories
        val possibleDirs = listOf(
            "$basePath/target/classes",
            "$basePath/build/classes/java/main",
            "$basePath/build/classes/kotlin/main",
            "$basePath/out/production/classes"
        )

        for (dirPath in possibleDirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                directoryField.text = dirPath
                scanDirectory(dir)
                break
            }
        }
    }

    private fun scanDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            Messages.showWarningDialog(
                project,
                GitRebaseBundle.message("arthas.error.invalid.directory"),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        // Clear existing rows
        tableModel.rowCount = 0

        // Scan for .class files
        val classFiles = mutableListOf<File>()
        scanForClassFiles(directory, classFiles)

        if (classFiles.isEmpty()) {
            Messages.showInfoMessage(
                project,
                GitRebaseBundle.message("arthas.info.no.class.files"),
                GitRebaseBundle.message("arthas.dialog.title")
            )
            return
        }

        // Populate table
        classFiles.sortedBy { it.name }.forEach { file ->
            val relativePath = file.relativeTo(directory).path
            val className = relativePath.replace(File.separator, ".").removeSuffix(".class")
            val size = formatFileSize(file.length())
            tableModel.addRow(arrayOf(className, file.absolutePath, size))
        }

        if (classFiles.isNotEmpty()) {
            classTable.setRowSelectionInterval(0, 0)
        }
    }

    private fun scanForClassFiles(directory: File, result: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanForClassFiles(file, result)
                file.isFile && file.extension == "class" -> result.add(file)
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun updateOKActionState() {
        isOKActionEnabled = selectedClassFile != null
    }

    override fun doOKAction() {
        if (selectedClassFile == null) {
            Messages.showWarningDialog(
                project,
                GitRebaseBundle.message("arthas.error.no.selection"),
                GitRebaseBundle.message("error.title")
            )
            return
        }
        super.doOKAction()
    }
}