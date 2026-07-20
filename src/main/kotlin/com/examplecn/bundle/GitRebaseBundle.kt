package com.examplecn.bundle

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.GitRebaseBundle"

object GitRebaseBundle : DynamicBundle(BUNDLE_NAME) {
    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}