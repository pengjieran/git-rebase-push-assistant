# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

An IntelliJ IDEA plugin (Kotlin) that adds a "变基并提交推送" (Rebase and Push) action to the Git commit UI. It fetches a target branch, rebases the current branch onto it, optionally commits selected changed files, force-pushes with `--force-with-lease`, and optionally creates a merge request.

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

The project compiles and builds successfully.

**GitLab MR Auto-creation Implemented** — The plugin now supports automatic Merge Request creation via GitLab API:
- When the user checks "推送后自动提交merge请求" in the rebase dialog, the plugin attempts to create an MR automatically
- On first use, prompts for a GitLab Personal Access Token (requires `api` scope)
- Token is securely stored using IntelliJ's `PasswordSafe` (system keychain/credential manager)
- Makes HTTP POST to `/api/v4/projects/{project_path}/merge_requests` with source/target branch, title, and description
- Parses remote URL to extract GitLab base URL and project path (supports SSH and HTTPS formats, including subgroups)
- On success, shows the MR URL; on failure, provides a pre-filled manual creation link
- JSON is constructed and parsed manually (no external dependencies) to avoid adding libraries

**Simplified settings** — `GitRebaseSettings` stores minimal user preferences (default target branch, autostash, notify on success).

**GitHub** — PR auto-creation is not yet implemented; returns a manual-creation link.

Test directory (`src/test/kotlin`) is still empty.

## Architecture

Three-layer structure under `src/main/kotlin/com/examplecn/`:

- **`action/`** — UI layer. `GitRebaseAndPushAction` is the `AnAction` entry point, enabled only when the project has at least one Git repository (via `GitUtil.getRepositoryManager`). It always operates on `repositories.firstOrNull()` — multi-repo projects are not disambiguated. It orchestrates two dialogs (`RebaseConfigDialog` for target branch + MR checkbox, `CommitFilesDialog` for selecting changed files and writing a commit message) and then runs the workflow in a `Task.Backgroundable` so Git operations don't block the UI thread.
- **`service/`** — business logic. `GitRebaseService` (`@Service(Service.Level.PROJECT)`) wraps all Git4Idea `GitLineHandler` calls (fetch, rebase, push, status, add, commit, remote lookup) and translates failures into `VcsException`. Fetched via `project.service<GitRebaseService>()`.
- **`config/`** — `GitRebaseSettings` (`@Service` + `PersistentStateComponent`) persists user preferences (default target branch, autostash, notify-on-success) and non-secret GitLab config (URL, project ID) to `gitRebasePlugin.xml`. The comment in the source notes that tokens are meant to go through the system credential store instead of this plain state, though no such credential-store integration exists yet.

Control flow for the main action: `actionPerformed` → show `RebaseConfigDialog` → if there are uncommitted changes, show `CommitFilesDialog` → `executeRebaseAndPushInBackground` runs fetch → rebase → (add + commit selected files) → force-push → optionally create MR, updating a `ProgressIndicator` at each step and surfacing errors via `Messages.showErrorDialog`.

`plugin.xml` (`src/main/resources/META-INF/`) is the extension/action registration point — the action is added to the `Vcs.CommitExecutor.Actions` group, meaning it surfaces in the Git commit dialog rather than as a toolbar/menu action elsewhere.