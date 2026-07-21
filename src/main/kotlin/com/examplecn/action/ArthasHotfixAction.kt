package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * Action to generate Arthas hotfix scripts from compiled .class files
 */
class ArthasHotfixAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
        e.presentation.text = GitRebaseBundle.message("arthas.action.name")
        e.presentation.description = GitRebaseBundle.message("arthas.action.description")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dialog = ArthasHotfixDialog(project)
        if (dialog.showAndGet()) {
            val selectedFile = dialog.selectedClassFile
            if (selectedFile != null) {
                generateHotfixScript(project, selectedFile)
            }
        }
    }

    private fun generateHotfixScript(project: Project, classFile: java.io.File) {
        try {
            val service = project.getService(com.examplecn.service.ArthasHotfixService::class.java)
            val scriptContent = service.generateHotfixScript(classFile)

            // Show dialog to save the script
            val saveDialog = ArthasScriptOutputDialog(project, classFile.nameWithoutExtension, scriptContent)
            saveDialog.show()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                GitRebaseBundle.message("arthas.error.generate.failed", e.message ?: "Unknown error"),
                GitRebaseBundle.message("error.title")
            )
        }
    }
}