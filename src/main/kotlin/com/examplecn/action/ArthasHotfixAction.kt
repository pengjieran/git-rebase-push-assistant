package com.examplecn.action

import com.examplecn.bundle.GitRebaseBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Action to generate Arthas hotfix scripts from compiled .class files
 * Supports selecting .java/.kt source files (will find corresponding .class) or .class files directly
 */
class ArthasHotfixAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project

        // Set text and description from resource bundle
        e.presentation.text = GitRebaseBundle.message("arthas.action.name")
        e.presentation.description = GitRebaseBundle.message("arthas.action.description")

        if (project == null) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
            return
        }

        // In file-context menus (editor / project view / editor tab), only show for
        // .java/.kt/.class selections. When no file is available (e.g. Tools menu),
        // keep it visible so it can still be triggered.
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val hasValidFile = selectedFiles?.any {
            !it.isDirectory && (it.extension == "class" || it.extension == "java" || it.extension == "kt")
        } ?: false

        val fromToolsMenu = selectedFiles.isNullOrEmpty()
        val show = fromToolsMenu || hasValidFile
        e.presentation.isVisible = show
        e.presentation.isEnabled = show
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        // Filter to valid files (Java/Kotlin source or .class files)
        val validFiles = selectedFiles.filter {
            !it.isDirectory && (it.extension == "class" || it.extension == "java" || it.extension == "kt")
        }

        if (validFiles.isEmpty()) {
            Messages.showWarningDialog(
                project,
                GitRebaseBundle.message("arthas.error.no.valid.file.selected"),
                GitRebaseBundle.message("error.title")
            )
            return
        }

        val errors = mutableListOf<String>()
        var successCount = 0

        // Process each selected file
        validFiles.forEach { virtualFile ->
            try {
                val classFile = when (virtualFile.extension) {
                    "class" -> File(virtualFile.path)
                    "java", "kt" -> findCompiledClassFile(project, virtualFile)
                    else -> null
                }

                if (classFile == null) {
                    errors.add("${virtualFile.name}: Could not find compiled .class file")
                } else if (!classFile.exists()) {
                    errors.add("${virtualFile.name}: Compiled class not found at ${classFile.path}")
                } else {
                    generateHotfixScript(project, classFile)
                    successCount++
                }
            } catch (ex: Exception) {
                errors.add("${virtualFile.name}: ${ex.message ?: "Unknown error"}")
            }
        }

        // Show summary if there were errors
        if (errors.isNotEmpty()) {
            val message = buildString {
                if (successCount > 0) {
                    append("Successfully generated $successCount script(s).\n\n")
                }
                append("Errors:\n")
                errors.forEach { append("• $it\n") }
                if (successCount == 0) {
                    append("\nTip: Make sure the project is built first (Build → Build Project)")
                }
            }
            Messages.showWarningDialog(project, message, GitRebaseBundle.message("error.title"))
        }
    }

    /**
     * Find the compiled .class file for a given source file (.java or .kt)
     * Supports multi-module projects by detecting the module and searching its output directories first
     */
    private fun findCompiledClassFile(project: Project, sourceFile: VirtualFile): File? {
        val fileName = sourceFile.nameWithoutExtension

        // Try to extract package path from source file
        val sourceContent = try {
            String(sourceFile.contentsToByteArray())
        } catch (e: Exception) {
            return null
        }

        val packagePath = extractPackagePath(sourceContent)

        // Try to find the module that contains this source file
        val module = ModuleUtilCore.findModuleForFile(sourceFile, project)

        if (module != null) {
            // IntelliJ's own knowledge of the module's compiled-classes output directory
            // (read-only extension; reflects whatever build system/import set it, incl. Gradle sub-modules)
            val compilerExtension = CompilerModuleExtension.getInstance(module)
            val compilerOutputPath = compilerExtension?.compilerOutputPath?.path

            if (compilerOutputPath != null) {
                val classFilePath = if (packagePath.isNotEmpty()) {
                    "$compilerOutputPath/$packagePath/$fileName.class"
                } else {
                    "$compilerOutputPath/$fileName.class"
                }

                val classFile = File(classFilePath)
                if (classFile.exists()) {
                    return classFile
                }
            }

            // Fall back to common build output directories relative to the module's content root
            val moduleBasePath = ModuleRootManager.getInstance(module).contentRoots.firstOrNull()?.path
            if (moduleBasePath != null) {
                val moduleSearchPaths = listOf(
                    "$moduleBasePath/target/classes",              // Maven
                    "$moduleBasePath/build/classes/java/main",     // Gradle (Java)
                    "$moduleBasePath/build/classes/kotlin/main",   // Gradle (Kotlin)
                    "$moduleBasePath/out/production/${module.name}", // IntelliJ
                    "$moduleBasePath/out/production/classes"       // Alternative
                )

                for (searchPath in moduleSearchPaths) {
                    val classFilePath = if (packagePath.isNotEmpty()) {
                        "$searchPath/$packagePath/$fileName.class"
                    } else {
                        "$searchPath/$fileName.class"
                    }

                    val classFile = File(classFilePath)
                    if (classFile.exists()) {
                        return classFile
                    }
                }
            }
        }

        // Fallback: search in project root (for single-module projects)
        val basePath = project.basePath ?: return null
        val projectSearchPaths = listOf(
            "target/classes",           // Maven
            "build/classes/java/main",  // Gradle (Java)
            "build/classes/kotlin/main", // Gradle (Kotlin)
            "out/production/${project.name}", // IntelliJ default
            "out/production/classes"    // Alternative IntelliJ
        )

        for (searchPath in projectSearchPaths) {
            val classFilePath = if (packagePath.isNotEmpty()) {
                "$basePath/$searchPath/$packagePath/$fileName.class"
            } else {
                "$basePath/$searchPath/$fileName.class"
            }

            val classFile = File(classFilePath)
            if (classFile.exists()) {
                return classFile
            }
        }

        return null
    }

    /**
     * Extract package path from Java/Kotlin source file
     * Example: "package com.example.foo" → "com/example/foo"
     */
    private fun extractPackagePath(sourceContent: String): String {
        val packagePattern = Regex("""package\s+([\w.]+)""")
        val match = packagePattern.find(sourceContent)
        return match?.groupValues?.get(1)?.replace('.', '/') ?: ""
    }

    private fun generateHotfixScript(project: Project, classFile: File) {
        val service = project.getService(com.examplecn.service.ArthasHotfixService::class.java)
        val scripts = service.generateHotfixScript(classFile)

        // Show dialog with copy/save options.
        // Display + save use the full script (with integrity checks);
        // clipboard copy uses the simplified variant without if/verification blocks.
        val outputDialog = ArthasScriptOutputDialog(
            project,
            classFile.nameWithoutExtension,
            scripts.full,
            scripts.clipboard,
            classFile
        )
        outputDialog.show()
    }
}