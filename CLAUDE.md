# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

An IntelliJ IDEA plugin (Kotlin) with two main features:
1. **Git Rebase & Push** — Adds a "变基并提交推送" action to the Git commit UI that automates fetch → rebase → commit → force-push (`--force-with-lease`) → optionally create GitLab MR
2. **Arthas Hotfix Generator** — Right-click action on `.java`/`.kt`/`.class` files that generates Base64-encoded shell scripts for Alibaba Arthas hot-patching in production

## Build and development commands

```bash
./gradlew buildPlugin      # Build the plugin; output at build/distributions/git-plugin-*.zip
./gradlew runIde            # Launch a sandboxed IDE instance with the plugin installed
./gradlew test              # Run tests (via the `check` lifecycle task)
./gradlew verifyPlugin      # Check plugin compatibility against target IntelliJ versions
```

Predefined Run/Debug configurations exist in `.run/` (Run Plugin, Run Tests, Run Verifications) that wrap the same Gradle tasks.

Requires JDK 17+. Target platform is IntelliJ IDEA 2025.3.5, declared via `intellijIdea("2025.3.5")` in `build.gradle.kts`, with `Git4Idea` as a bundled plugin dependency.

## Build status

The project compiles and builds successfully. `src/test/kotlin` is empty — no tests exist yet.

**GitHub PR** — auto-creation is not yet implemented; returns a manual-creation link.

## Architecture

Package root: `src/main/kotlin/com/examplecn/`

**`action/`** — UI entry points (two registered actions):
- `GitRebaseAndPushAction` — surfaces in the Git commit dialog (`Vcs.CommitExecutor.Actions` group). Enabled only when the project has at least one Git repository. Always operates on `repositories.firstOrNull()` — multi-repo projects are not disambiguated. Launches `UnifiedRebaseDialog`, which combines all configuration and progress display. After the user confirms, runs all steps synchronously on the EDT: add + commit → fetch → rebase → force-push → optionally create MR. Progress text updates inline; dialog auto-closes 2 seconds after completion.
- `ArthasHotfixAction` — surfaces in `ToolsMenu` and `ProjectViewPopupMenu`. Accepts `.java`, `.kt`, or `.class` file selections. For source files, locates the corresponding compiled `.class` by searching common output directories (`target/classes`, `build/classes/kotlin/main`, `out/production/…`). Delegates to `ArthasHotfixService`, then shows `ArthasScriptOutputDialog` with copy/save options.

**`service/`** — four project-level services (`@Service(Service.Level.PROJECT)`):
- `GitRebaseService` — wraps all Git4Idea `GitLineHandler` calls (fetch, rebase, push, status, add, commit, remote lookup). All Git operations must run inside `runReadAction` (reads) or `runWriteAction` (mutating) to satisfy EDT threading requirements.
- `OpenAIService` — calls the OpenAI-compatible chat completions API using `HttpURLConnection` (no external libraries). Builds the prompt from the git diff and generates a conventional commit message in Chinese.
- `MergeRequestService` — POSTs to `/api/v4/projects/{project_path}/merge_requests`. Parses the project's remote URL (SSH and HTTPS, including subgroups) to derive the GitLab base URL and project path. GitLab Personal Access Token is stored in IntelliJ's `PasswordSafe` (system keychain).
- `ArthasHotfixService` — reads a `.class` file, Base64-encodes it, and generates a self-contained bash script that decodes and writes the file to `/tmp` then prints the Arthas `retransform` command.

**`config/`** — `GitRebaseSettings` (`PersistentStateComponent`) persists non-secret preferences (default target branch, autostash, notify-on-success, OpenAI endpoint/model, non-secret GitLab config) to `gitRebasePlugin.xml`. `GitRebaseSettingsConfigurable` renders the settings UI under `Tools > Git Rebase & Push`.

**`bundle/`** — `GitRebaseBundle` wraps `ResourceBundle` lookups. Message properties files live in `src/main/resources/messages/` with a `_zh_CN` variant for Chinese localization.

**Threading model:** All Git operations use `ApplicationManager.getApplication().run{Read,Write}Action(Computable {...})`. The entire rebase-and-push workflow executes synchronously on the EDT — no background coroutines or `ProgressManager` tasks.

**No external runtime dependencies** — JSON construction and parsing is done manually with string manipulation to avoid adding libraries to the plugin classpath.