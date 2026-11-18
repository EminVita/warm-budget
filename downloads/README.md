# 签名安装包暂存区

仅存放准备发布的 APK，不存放任何签名密钥。当前测试渠道将本机签好的小体积安装包提交到这里，由 Publish signed update 工作流发布至 GitHub Releases。

每次发布需递增 app/build.gradle.kts 的 versionCode 和 versionName，本地构建并使用原签名签名，将安装包命名为 living-budget-版本号.apk，同时更新根目录 update.json。发布过的版本不覆盖，发现问题必须递增版本发布。

应用检查地址固定读取 main/update.json，下载地址指向对应 Release 资源。新版本已上架前不要分发安装包；此前已配置检查地址的客户端在发布窗口短时间内可能先看到版本提示，待 Release 工作流成功后下载链接才可用。

这是沿用现有测试签名的测试分发渠道。正式发行前需确定长期签名与独立发布流程，避免持续提交安装包导致 Git 历史增长。
