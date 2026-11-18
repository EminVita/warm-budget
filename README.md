# 生活费 · Android

Kotlin + Jetpack Compose 原生安卓应用。最低 Android 8.0，本地存储、无需登录。

## 修改文案

全部页面标签、验证提示和通知模板集中在 [`app/src/main/assets/copy.zh-CN.json`](app/src/main/assets/copy.zh-CN.json)。保留字段名，只修改对应文字。通知模板中的 `%s`、`%d` 必须保留，且顺序不变。

安装后也可以通过「设置 → 页面文案 → 导出文案模板」获取 JSON，修改后导入，立即生效。支持只导入部分字段，未提供的字段使用默认值；导入会替换上一次导入的覆盖内容。修改失败会保留原文案。系统桌面显示的应用名称另在 `res/values/strings.xml` 设置，需要重新打包。

## 布局、状态和业务分离

| 修改目标 | 文件 |
| --- | --- |
| 首页和设置的区块顺序、排版 | `app/src/main/java/com/livingbudget/app/ui/Screens.kt` |
| 新增、历史、详情等弹层 | `app/src/main/java/com/livingbudget/app/ui/Panels.kt` |
| 配色、图标、卡片 | `ui/Theme.kt`、`ui/Components.kt` |
| 页面需要的数据及事件契约 | `presentation/Contract.kt` |
| 状态转换、校验、保存等操作 | `presentation/BudgetViewModel.kt` |
| 费用、日期、预算的纯计算 | `core/src/main/kotlin/com/livingbudget/core/Ledger.kt` |
| 数据库与导入导出 | `data/` |
| 通知调度及去重 | `reminders/ReminderWorker.kt` |

布局组件只接收 `UiState` 和 `(Action) -> Unit`。移动按钮、换卡片、拆分布局时继续发送同一事件，例如 `Action.SaveExpense`、`Action.OpenHistory`，无需修改数据库或业务接口。添加全新的业务能力时才需要扩展事件契约。页面文案不参与事件匹配、枚举判断或数据库索引。

日期与金额来自真实本地数据，首次安装为空，不塞入示例账单。界面内的横线、货币符号、日期格式、版本号和用户输入是结构／数据，不属于页面文案。

## 开发与构建

需要 JDK 17、Gradle 8.13、Android SDK Platform 36 和对应 Build Tools。依赖版本固定在根目录及 app 的 Gradle 文件中，优先使用已锁定组合，升级时验证兼容性。

Windows 可运行 `scripts/bootstrap.ps1` 下载官方工具到被忽略的 `.tools`。如网络较慢，`scripts/download-tools.py` 支持分段下载 Gradle 与 SDK 命令行工具。安装 SDK 前阅读并接受 Google Android SDK 许可，之后执行：

```powershell
# 在配置好 JAVA_HOME、ANDROID_HOME 后：
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" 'platforms;android-36' 'build-tools;36.0.0'
./scripts/build.ps1
```

也可用 Android Studio 打开项目并配置 SDK，通过随工程提供的 Gradle Wrapper 构建。自动构建入口为 `scripts/build.ps1`，包括核心测试、Android 数据测试、两种构建和 Lint。核心测试和 Robolectric 数据测试无需手机，后者目前使用模拟 Android 15（API 35）运行。

Debug APK 在 `app/build/outputs/apk/debug/`。裁剪后的 release APK 默认未签名，不能直接安装。运行 `scripts/package-preview.ps1` 可用本机测试密钥签出 `dist/living-budget-0.1.0-preview.apk`，仅用于体验；正式发行应使用独立且长期保管的发布签名。切换签名时不能直接覆盖旧测试版，请先导出备份。

## 到期提醒

每项订阅选择取消自动续费或准备缴费。基准日默认结束日期，也可设置实际扣款日期。在之前第 3、2、1 天，每天最多一条通知；用户可指定提醒时刻，默认上海时间 09:00。全局开关初始关闭，用户主动开启时请求通知权限。

通过 WorkManager 每小时检查及打开应用时校正，无常驻服务。系统省电、关机或强行停止应用可能导致延迟或漏发，不承诺精确时刻，不补发之前多天的消息。点击通知打开订阅详情，可停止本轮提醒；不会替用户操作服务商的自动续费。重启后后台任务由 WorkManager 恢复。

## 备份与更新

备份包含费用、预算和更正快照，不包含页面文案、系统权限、主题和通知发送记录。恢复前显示确认提示，整体事务替换；请先导出当前数据。通知发送记录不随备份迁移，恢复后当天符合条件的订阅可能再次提醒。

更新地址配置在 `app/src/main/assets/app-config.json` 的 `updateManifestUrl`。默认为空，点击检查会明确提示未配置，不能伪装成“已是最新”。发布时使用 HTTPS 直连 JSON（格式见 `update-manifest.example.json`，不使用示例域名），下载链接可指向 GitHub Releases。检测到新版本后经用户点击，用浏览器下载，安装由系统确认。

正式发布前固定 applicationId、配置私有签名、递增 versionCode，保管签名密钥；不要提交密钥。发行构建开启 R8 和资源裁剪，体积需以正式构建实测。数据库不配置破坏性迁移，后续 schema 更新必须提供 Migration 及保留数据的验证。

## 0.2.0 已确认调整

本节优先于早期设计。预算只填金额并保存，不再选择范围；更新当前所选月份并作为后续默认值，之前月份不改写。

订阅与日常花费均保留名称、金额、期限、备注和提醒，删除付款日期、付款状态及分类。旧数据库字段和备份保持兼容，但不参与新界面统计；新保存记录的内部日期取开始日。单日只显示日期；月／季／年只输入开始日期，结束日自动推算；自定义填写起止日期。

今日成本仅累计今天有效记录的日额。结束日包含当天，次日零点归零。本月成本累计全部记录与该月的交集金额，已结束记录的本月成本不删除。首页将本月已付改为本月成本，预算余量按预算减本月成本计算，不再显示待付。

累计分摊金额四舍五入到分，再通过相邻累计值之差得到每日和每月金额，保证总额守恒。¥300、2026-03-20 至 2026-04-02 共 14 天，3 月 ¥257.14、4 月 ¥42.86；4 月 3 日起不贡献今日成本。
