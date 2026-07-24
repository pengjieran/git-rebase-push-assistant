# Git Enhancement Plugin

[![Version](https://img.shields.io/badge/version-1.0.3-orange)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![JetBrains](https://img.shields.io/badge/IntelliJ-2025.3.5-blue)](https://www.jetbrains.com/idea/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)]()

A powerful IntelliJ IDEA plugin that enhances Git workflows with intelligent rebase & push, AI-powered commit message generation, and Arthas hotfix script generation.

[中文文档](README_CN.md)

## ✨ Core Features

### 🔄 Smart Rebase & Push
- **One-Click Operation**: Automatically executes fetch → rebase → push workflow
- **Safe Push**: Uses `--force-with-lease` to prevent overwriting others' commits
- **Auto Commit**: Automatically commits all changed files before rebasing
- **Branch Suggestions**: Smart detection and recommendation of master/main/develop branches
- **Real-time Progress**: Displays operation progress and user-friendly error messages

### 🤖 AI Commit Message Generation
- **Intelligent Analysis**: Automatically generates commit messages based on file changes and git diff
- **Configurable**: Supports custom OpenAI-compatible APIs (Base URL, Model ID, API Key)
- **Connection Test**: Settings page provides API connection testing
- **Optimized Prompts**: Generates commit messages following best practices

### 🔥 Arthas Hotfix Scripts
- **Quick Generation**: Right-click `.class` files to generate hotfix scripts
- **Base64 Encoding**: Automatically encodes class files and generates complete shell scripts
- **Batch Support**: Process multiple `.class` files simultaneously
- **Flexible Output**: Copy to clipboard or save as executable script

### 🚀 GitLab Integration
- **Auto Create MR**: Automatically creates Merge Requests after push
- **Secure Storage**: Stores Personal Access Token in system keychain using `PasswordSafe`
- **Smart Parsing**: Auto-detects GitLab URL and project path (supports SSH/HTTPS, subgroups)
- **Graceful Fallback**: Provides pre-filled manual creation link on API failure

## 📦 Installation

### Option 1: Build from Source

```bash
# Clone repository
git clone <repository-url>
cd git-plugin

# Build plugin
./gradlew buildPlugin

# Output: build/distributions/git-plugin-*.zip
```

### Option 2: Install to IDEA

1. Open `Preferences/Settings` → `Plugins`
2. Click ⚙️ → `Install Plugin from Disk...`
3. Select the generated ZIP file
4. Restart IDEA

## 🚀 Quick Start

### 1️⃣ Rebase and Push

**Use Case**: Sync feature branch with latest master

1. Open Git commit window (`Cmd+K` / `Ctrl+K`)
2. Click **"Rebase and Push"** button (or via menu `Git` → `Rebase and Push`)
3. Select target branch (e.g., `master`)
4. Optionally check **"Automatically create merge request after push"**
5. Click **"Rebase and Push"**

The plugin will automatically execute:
```bash
# If there are uncommitted changes
git add .
git commit -m "Your commit message"

# Rebase workflow
git fetch origin master
git rebase origin/master
git push --force-with-lease origin feature/your-branch

# If auto-create MR is checked
# Calls GitLab API to create Merge Request
```

### 2️⃣ AI-Generated Commit Messages

**Prerequisites**:

1. Open `Preferences/Settings` → `Tools` → `Git Rebase & Push`
2. Configure OpenAI API:
   - **Base URL**: API endpoint (e.g., `https://api.openai.com/v1`)
   - **Model ID**: Model name (e.g., `gpt-4` or `gpt-3.5-turbo`)
   - **API Key**: Your API key
3. Click **"Test Connection"** to verify configuration
4. Click **"Apply"** to save

**Usage**:

1. In the rebase dialog's commit message section
2. Click **"AI Generate"** button
3. Wait for AI to analyze changes and generate commit message
4. Modify the generated message as needed

**Configuration Examples**:

```properties
# OpenAI Official
Base URL: https://api.openai.com/v1
Model ID: gpt-4
API Key: sk-...

# Azure OpenAI
Base URL: https://your-resource.openai.azure.com/openai/deployments/your-deployment
Model ID: gpt-4
API Key: your-azure-key

# Local Model (Ollama)
Base URL: http://localhost:11434/v1
Model ID: qwen:7b
API Key: (leave empty)
```

### 3️⃣ Generate Arthas Hotfix Scripts

**Use Case**: Generate Arthas hotfix scripts for compiled Java classes

1. In project view, navigate to build output directory (e.g., `target/classes` or `build/classes`)
2. Select one or more `.class` files
3. Right-click → **"Generate Arthas Hotfix Script"**
4. In the dialog, choose:
   - **Copy to Clipboard**: Quick copy of script content
   - **Save to File**: Save as executable `.sh` script
   - **Close**: View only

**Generated Script Example**:

```bash
#!/bin/bash
# Arthas Hotfix Script for UserService
# Generated: 2026-07-21T12:34:56Z

# The script will automatically:
# 1. Decode Base64-encoded class file
# 2. Create temporary .class file
# 3. Provide Arthas retransform command

# Usage:
# 1. Upload script to target server
# 2. chmod +x UserService_hotfix_*.sh
# 3. ./UserService_hotfix_*.sh
# 4. Attach Arthas and execute retransform command
```

## ⚙️ Configuration

### Git Rebase Settings

Open `Preferences/Settings` → `Tools` → `Git Rebase & Push`

| Setting | Description | Default |
|---------|-------------|---------|
| Default Target Branch | Default target branch | `master` |
| Auto Stash | Automatically stash uncommitted changes | ✅ Enabled |
| Notify on Success | Show notification on success | ✅ Enabled |

### OpenAI API Configuration

| Setting | Description | Required |
|---------|-------------|----------|
| Base URL | API endpoint address | ✅ |
| Model ID | Model name to use | ✅ |
| API Key | API key | ✅ |

### GitLab Token Configuration

On first MR creation, you'll be prompted for a Personal Access Token:

1. Visit GitLab: `User Settings` → `Access Tokens`
2. Create new token with `api` scope
3. Copy token and paste into plugin prompt
4. Token will be securely stored in system keychain

## 🏗️ Project Structure

```
git-plugin/
├── src/main/kotlin/com/examplecn/
│   ├── action/                    # UI Layer
│   │   ├── GitRebaseAndPushAction.kt         # Main action entry
│   │   ├── UnifiedRebaseDialog.kt            # Rebase dialog
│   │   ├── ArthasHotfixAction.kt             # Arthas script generation
│   │   └── ArthasScriptOutputDialog.kt       # Script output dialog
│   ├── service/                   # Business Logic Layer
│   │   ├── GitRebaseService.kt               # Git operations service
│   │   ├── MergeRequestService.kt            # MR/PR creation service
│   │   ├── OpenAIService.kt                  # OpenAI API service
│   │   └── ArthasHotfixService.kt            # Arthas script service
│   ├── config/                    # Configuration Layer
│   │   ├── GitRebaseSettings.kt              # Settings persistence
│   │   └── GitRebaseSettingsConfigurable.kt  # Settings UI
│   └── bundle/                    # Internationalization
│       └── GitRebaseBundle.kt                # Resource bundle accessor
├── src/main/resources/
│   ├── META-INF/plugin.xml                   # Plugin manifest
│   └── messages/
│       ├── GitRebaseBundle.properties        # English resources
│       └── GitRebaseBundle_zh_CN.properties  # Chinese resources
└── build.gradle.kts                          # Build configuration
```

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9+
- **Platform**: IntelliJ Platform 2025.3.5
- **Dependencies**: Git4Idea (built-in)
- **Build Tool**: Gradle 8.x
- **JDK**: 17+

## 🧪 Development

### Run Test IDE

```bash
./gradlew runIde
```

Launches an IDEA test instance with the plugin installed.

### Run Tests

```bash
./gradlew test
```

### Build Plugin

```bash
./gradlew buildPlugin
```

Output location: `build/distributions/git-plugin-*.zip`

### Verify Plugin Compatibility

```bash
./gradlew verifyPlugin
```

## 📝 Usage Examples

### Scenario 1: Sync with Main Branch

```bash
# Current branch: feature/user-auth
# Goal: Sync with latest master

# Steps:
1. Cmd+K to open commit window
2. Click "Rebase and Push"
3. Select target branch: master
4. Click OK

# Result:
✓ Successfully rebased onto master
✓ Pushed to remote repository
```

### Scenario 2: Submit PR with Auto MR

```bash
# Current branch: feature/new-api
# Goal: Rebase onto develop and create MR

# Steps:
1. Ensure code is committed
2. Click "Rebase and Push"
3. Select target branch: develop
4. Check "Automatically create merge request after push"
5. Click OK

# Result:
✓ Rebased onto develop
✓ Successfully pushed
✓ Auto-created MR: feature/new-api → develop
✓ MR link displayed in notification
```

### Scenario 3: Production Hotfix

```bash
# Scenario: Bug found in production, needs urgent hotfix

# Steps:
1. Fix code locally and compile
2. Find modified .class file in target/classes
3. Right-click → "Generate Arthas Hotfix Script"
4. Save as hotfix.sh
5. Upload to production server
6. Execute script and use Arthas retransform

# Result:
✓ Bug fixed without service restart
✓ Minimized service interruption
```

## ❓ FAQ

### Q1: What to do when rebase fails?

**A**: Rebase failures are usually due to conflicts. The plugin will show error messages. You need to:

1. Manually resolve conflicts: `git status` to view conflicting files
2. Edit conflicting files and mark as resolved: `git add <file>`
3. Continue rebase: `git rebase --continue`
4. Push again: `git push --force-with-lease`

### Q2: Why does AI generation fail?

**A**: Check the following:

- ✅ Is the API Key correct?
- ✅ Is the Base URL accessible?
- ✅ Does the Model ID exist?
- ✅ Is network connection normal?
- ✅ Is API quota sufficient?

Use the "Test Connection" button to verify configuration.

### Q3: Where is the GitLab Token stored?

**A**: Tokens are stored using IntelliJ's `PasswordSafe`, location varies by OS:

- **macOS**: Keychain Access
- **Windows**: Windows Credential Manager
- **Linux**: System keyring or encrypted file

### Q4: Does it support GitHub?

**A**: Currently only GitLab auto-MR creation is supported. GitHub PR auto-creation is not yet implemented, but a pre-filled manual creation link is provided.

### Q5: How to use Arthas scripts?

**A**: Generated scripts include detailed instructions. Basic workflow:

```bash
# 1. Upload script to server
scp hotfix.sh user@server:/tmp/

# 2. Grant execute permission
chmod +x /tmp/hotfix.sh

# 3. Execute script (generates .class file)
/tmp/hotfix.sh

# 4. Start Arthas and attach to Java process
java -jar arthas-boot.jar

# 5. Execute retransform in Arthas
retransform /tmp/YourClass_*.class

# 6. Verify fix is effective
```

## 🐛 Known Limitations

- ❌ Multi-repo projects only operate on first repository
- ❌ GitHub PR auto-creation not yet implemented
- ❌ Interactive rebase (e.g., `-i`) not supported
- ❌ Arthas scripts only support single class files (no inner class splitting)

## 🔮 Roadmap

- [ ] Support GitHub PR auto-creation
- [ ] Support multi-repo project selection
- [ ] Add interactive conflict resolution wizard
- [ ] Support custom AI prompt templates
- [ ] Arthas scripts support multi-class merging
- [ ] Add rebase history tracking
- [ ] Support Gitee and other platforms

## 📄 License

Apache License 2.0

## 🙏 Acknowledgments

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij)
- [Git4Idea](https://github.com/JetBrains/intellij-community/tree/master/plugins/git4idea)
- [Arthas](https://arthas.aliyun.com/)

---

**Note**: For Chinese documentation, see [README_CN.md](README_CN.md)