package com.examplecn.bundle

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.GitRebaseBundle"

object GitRebaseBundle {
    private val INSTANCE = DynamicBundle(GitRebaseBundle::class.java, BUNDLE_NAME)

    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
        return INSTANCE.getMessage(key, *params)
    }
}