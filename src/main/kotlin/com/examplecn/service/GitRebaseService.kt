package com.examplecn.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

/**
 * Git变基操作服务
 */
@Service(Service.Level.PROJECT)
class GitRebaseService(private val project: Project) {

    /**
     * 拉取远程分支最新内容
     */
    fun fetchRemoteBranch(repository: GitRepository, branch: String) {
        val handler = GitLineHandler(project, repository.root, GitCommand.FETCH)
        handler.addParameters("origin", branch)
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            throw VcsException("Failed to fetch branch $branch: ${result.errorOutputAsJoinedString}")
        }
    }

    /**
     * 将当前分支变基到目标分支
     */
    fun rebaseOnto(repository: GitRepository, targetBranch: String) {
        val handler = GitLineHandler(project, repository.root, GitCommand.REBASE)
        handler.addParameters("origin/$targetBranch")
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            throw VcsException("Rebase failed: ${result.errorOutputAsJoinedString}")
        }
    }

    /**
     * 强制推送当前分支（使用--force-with-lease保护）
     */
    fun forcePushBranch(repository: GitRepository, branch: String) {
        val handler = GitLineHandler(project, repository.root, GitCommand.PUSH)
        handler.addParameters("--force-with-lease", "origin", branch)
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            throw VcsException("Push failed: ${result.errorOutputAsJoinedString}")
        }
    }

    /**
     * 获取所有远程分支
     */
    fun getRemoteBranches(repository: GitRepository): List<String> {
        return try {
            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction<List<String>> {
                repository.branches.remoteBranches
                    .mapNotNull { branch ->
                        val branchName = branch.name.removePrefix("origin/")
                        branchName.takeIf { it.isNotEmpty() }
                    }
                    .distinct()
                    .sorted()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取未提交的变动文件（包括未暂存和已暂存）
     */
    fun getChangedFiles(repository: GitRepository): List<String> {
        return try {
            val handler = GitLineHandler(project, repository.root, GitCommand.STATUS)
            handler.addParameters("--porcelain")
            val result = Git.getInstance().runCommand(handler)

            if (result.success()) {
                result.output
                    .filter { it.isNotBlank() }
                    .map { line ->
                        line.substring(3).trim()
                    }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加文件到暂存区
     */
    fun addFiles(repository: GitRepository, files: List<String>) {
        val handler = GitLineHandler(project, repository.root, GitCommand.ADD)
        handler.addParameters(files)
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            throw VcsException("Failed to add files: ${result.errorOutputAsJoinedString}")
        }
    }

    /**
     * 提交变更
     */
    fun commitChanges(repository: GitRepository, message: String) {
        val handler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        handler.addParameters("-m", message)
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            throw VcsException("Commit failed: ${result.errorOutputAsJoinedString}")
        }
    }

    /**
     * 获取origin远程仓库URL
     */
    fun getRemoteUrl(repository: GitRepository): String? {
        return repository.remotes.firstOrNull { it.name == "origin" }?.firstUrl
            ?: repository.remotes.firstOrNull()?.firstUrl
    }

    /**
     * 执行完整的变基流程
     */
    fun executeRebaseWorkflow(
        repository: GitRepository,
        targetBranch: String,
        currentBranch: String
    ): RebaseResult {
        return try {
            fetchRemoteBranch(repository, targetBranch)
            rebaseOnto(repository, targetBranch)
            forcePushBranch(repository, currentBranch)
            RebaseResult.Success
        } catch (e: VcsException) {
            RebaseResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class RebaseResult {
    object Success : RebaseResult()
    data class Error(val message: String) : RebaseResult()
}