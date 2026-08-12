# IntelliJ 插件自动发布配置指南

本仓库已配置 GitHub Actions 实现插件的自动构建和发布。

## 🔧 配置步骤

### 1. 获取 JetBrains Marketplace Token

首次发布需要手动提交插件到 JetBrains Marketplace：

1. 访问 [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. 使用 JetBrains Account 登录
3. 首次提交：
   - 运行 `./gradlew buildPlugin` 构建插件
   - 手动上传 `build/distributions/` 中的 `.zip` 文件到 Marketplace
   - 等待 JetBrains 审核通过（通常 1-3 个工作日）

4. 获取 Permanent Token：
   - 插件审核通过后，访问 [https://plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens)
   - 点击 **Generate New Token**
   - 复制生成的 Token（格式：`perm:xxx...`）

### 2. 配置 GitHub Secret

1. 进入 GitHub 仓库的 **Settings → Secrets and variables → Actions**
2. 点击 **New repository secret**
3. 添加以下 Secret：
   - **Name**: `PUBLISH_TOKEN`
   - **Value**: 粘贴你的 JetBrains Marketplace Token

### 3. 发布新版本

配置完成后，每次发布只需执行：

```bash
# 1. 更新版本号和 CHANGELOG
# 编辑 gradle.properties: version = 1.0.6
# 编辑 CHANGELOG.md: 添加新版本说明

# 2. 提交更改
git add .
git commit -m "chore: bump version to 1.0.6"
git push

# 3. 创建并推送标签（触发自动发布）
git tag v1.0.6
git push origin v1.0.6
```

推送标签后，GitHub Actions 会自动：
- ✅ 构建插件
- ✅ 运行测试和验证
- ✅ 发布到 JetBrains Marketplace
- ✅ 创建 GitHub Release 并上传构建产物

## 📁 工作流说明

### `.github/workflows/build.yml`
- **触发时机**: 推送到 `main` 或 `feature/*` 分支，或提交 PR
- **作用**: 持续集成，构建和测试插件
- **产物**: 临时保存构建的 `.zip` 文件（7 天）

### `.github/workflows/release.yml`
- **触发时机**: 推送以 `v` 开头的标签（如 `v1.0.5`）
- **作用**: 自动发布到 JetBrains Marketplace 和 GitHub Release
- **产物**: 永久保存在 GitHub Release 中

## 🔍 查看发布状态

1. **GitHub Actions**: 仓库的 **Actions** 标签页查看构建日志
2. **JetBrains Marketplace**: 访问你的插件页面查看发布状态
3. **GitHub Releases**: 仓库的 **Releases** 标签页查看发布历史

## 🚨 注意事项

1. **首次发布**: 必须手动提交并等待审核通过后才能使用自动发布
2. **版本号**: 确保 `gradle.properties` 中的版本号与 Git 标签一致
3. **CHANGELOG**: 发布前务必更新 CHANGELOG.md
4. **Token 安全**: 切勿在代码中硬编码 Token，只通过 GitHub Secrets 配置

## 📖 参考文档

- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [GitHub Actions 文档](https://docs.github.com/en/actions)