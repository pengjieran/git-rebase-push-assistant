#!/bin/bash

# Git变基推送插件 - 演示脚本
# 此脚本创建一个测试Git仓库来演示插件功能

set -e

echo "🚀 Git变基推送插件功能演示"
echo "=============================="

# 创建临时测试目录
DEMO_DIR="/tmp/git-rebase-demo-$$"
echo ""
echo "📁 创建测试仓库: $DEMO_DIR"
mkdir -p "$DEMO_DIR"
cd "$DEMO_DIR"

# 初始化Git仓库
echo ""
echo "🔧 初始化Git仓库..."
git init
git config user.name "Demo User"
git config user.email "demo@example.com"

# 创建初始提交
echo "主分支初始内容" > README.md
git add README.md
git commit -m "Initial commit"

echo "版本1.0" > version.txt
git add version.txt
git commit -m "Add version 1.0"

# 创建master分支（模拟主干）
echo ""
echo "🌿 创建master分支..."
git branch -M master

# 在master上添加一些提交
echo "功能A" > feature-a.txt
git add feature-a.txt
git commit -m "Add feature A"

echo "功能B" > feature-b.txt
git add feature-b.txt
git commit -m "Add feature B"

# 创建feature分支
echo ""
echo "🌿 创建feature分支..."
git checkout -b feature/new-feature HEAD~2

# 在feature分支上开发
echo "新功能X" > feature-x.txt
git add feature-x.txt
git commit -m "Implement feature X"

echo "新功能Y" > feature-y.txt
git add feature-y.txt
git commit -m "Implement feature Y"

# 显示当前状态
echo ""
echo "📊 当前Git状态："
echo "==============="
echo ""
echo "分支列表："
git branch -a
echo ""
echo "当前分支: $(git branch --show-current)"
echo ""
echo "提交历史 (feature):"
git log --oneline --graph --all -10

# 模拟插件操作
echo ""
echo "🔄 模拟插件执行的操作："
echo "======================"
echo ""
echo "1️⃣  选择目标分支: master"
echo "2️⃣  Fetch远程 (模拟): git fetch origin master"
echo "3️⃣  变基操作: git rebase master"
echo ""
echo "执行变基..."
git rebase master

echo ""
echo "✅ 变基完成！"
echo ""
echo "4️⃣  推送操作 (模拟): git push --force-with-lease origin feature/new-feature"
echo ""

# 显示变基后的历史
echo "📊 变基后的提交历史："
echo "===================="
git log --oneline --graph --all -10

echo ""
echo "🎯 关键观察点："
echo "=============="
echo "- feature分支的提交(X, Y)现在基于最新的master"
echo "- feature分支包含了master的新提交(A, B)"
echo "- 提交历史保持线性"
echo ""

# 显示文件列表
echo "📁 feature分支的文件："
ls -la

echo ""
echo "🎉 演示完成！"
echo ""
echo "这个仓库位于: $DEMO_DIR"
echo "您可以使用以下命令查看:"
echo "  cd $DEMO_DIR"
echo "  git log --oneline --graph --all"
echo ""
echo "清理测试仓库:"
echo "  rm -rf $DEMO_DIR"
