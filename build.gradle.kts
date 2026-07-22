import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Changelog Plugin Configuration
changelog {
    version.set(providers.gradleProperty("version"))
    path.set(file("CHANGELOG.md").canonicalPath)
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here, for example:
        bundledPlugin("Git4Idea")
    }

}

// Plugin Configuration
intellijPlatform {
    pluginConfiguration {
        // Inject changelog into plugin.xml <change-notes>
        changeNotes.set(provider {
            changelog.renderItem(
                changelog.getLatest(),
                org.jetbrains.changelog.Changelog.OutputType.HTML
            )
        })
    }

    // Publishing configuration (for future automated releases)
    // First-time submission must be manual via plugins.jetbrains.com
    // After approval, set PUBLISH_TOKEN env var and run: ./gradlew publishPlugin
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}
