package com.examplecn.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * Git变基插件配置
 * 用于存储用户偏好设置
 */
@Service(Service.Level.PROJECT)
@State(
    name = "GitRebasePluginSettings",
    storages = [Storage("gitRebasePlugin.xml")]
)
class GitRebaseSettings : PersistentStateComponent<GitRebaseSettings.State> {

    private var myState = State()

    data class State(
        // 用户偏好
        var defaultTargetBranch: String = "develop",
        var useAutoStash: Boolean = true,
        var notifyOnSuccess: Boolean = true
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): GitRebaseSettings {
            return project.getService(GitRebaseSettings::class.java)
        }
    }
}