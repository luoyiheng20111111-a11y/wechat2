# WeChat

一个 SwiftUI 微信风格聊天 Demo，已经整理成可以直接放进 GitHub 的 Xcode 项目，并附带 GitHub Actions 自动构建流程。

## 功能

- SwiftUI 微信风格聊天界面
- DeepSeek AI 对话
- 聊天记录本地保存
- DeepSeek 主动消息
- 主动消息时间可以在 App 的“我 -> AI 设置”中自己填写
- AI 生成主动消息后，先写入聊天记录，再发送本地通知
- GitHub Actions 自动构建 iOS Release App，并生成 unsigned IPA artifact

## 1. 上传 GitHub

把整个 `WeChat_GitHub` 文件夹上传到仓库根目录，然后：

```bash
git init
git add .
git commit -m "Initial GitHub Actions version"
git branch -M main
git remote add origin <你的GitHub仓库地址>
git push -u origin main
```

## 2. 配置 DeepSeek API Key 和主动消息时间

安装 App 后打开：

`我 -> AI 设置`

可以直接填写：

- DeepSeek API Key
- 自动回复间隔（分钟）

点击“保存设置”后立即生效。主动消息会按照新的分钟数重新计时。

API Key 不再写进 Swift 源码、Info.plist 或 GitHub Actions Secret。

> 注意：API Key 保存在本机 App 数据中。iOS 客户端直接调用 DeepSeek 的方案适合个人项目测试；正式产品建议使用自己的服务器作为 API 代理。

## 3. 自动构建

推送到 `main` 后，GitHub Actions 会自动运行：

`Actions -> Build iOS IPA`

构建成功后，在对应 workflow run 的 `Artifacts` 中可以看到：

`WeChat-unsigned-IPA`

也可以在 GitHub Actions 页面手动点击 `Run workflow`。

## 4. 关于 unsigned IPA

当前 Actions 生成的是 **未签名 IPA**。

它主要用于验证项目能够在 GitHub 的 macOS + Xcode 环境中成功构建。

如果目标是直接安装到自己的 iPhone/iPad，还需要 Apple Developer 签名、Provisioning Profile 和导出流程。下一步可以再把 GitHub Actions 升级成自动签名并输出可安装 IPA。

## 5. 项目结构

```text
WeChat_GitHub/
├── .github/
│   └── workflows/
│       └── build.yml
├── WeChat/
│   ├── Assets.xcassets/
│   │   └── administerphoto.imageset/
│   ├── WeChat.swift
│   ├── ContentView.swift
│   ├── ChatView.swift
│   ├── ChatStorage.swift
│   ├── AIService.swift
│   └── ProactiveMessageManager.swift
├── WeChat.xcodeproj/
├── .gitignore
└── README.md
```
