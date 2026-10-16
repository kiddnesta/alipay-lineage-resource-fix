# 支付宝 LineageOS 资源冲突修复

Alipay LineageOS Resource Fix — 一个仅作用于支付宝主进程的 LSPosed 模块，绕过聊天资源与 LineageOS 平台资源之间的 `0x3f` 资源 ID 冲突。

**当前版本仅支持 Android 13 / API 33。** 尚未验证其他机型、支付宝版本和完整业务功能。

## 下载与安装

从本仓库 **Releases** 下载 APK，安装后在 LSPosed 启用“支付宝资源冲突修复”。作用域仅勾选 **支付宝**（`com.eg.android.AlipayGphone`），然后完全结束支付宝进程并重新打开。

模块没有桌面图标，在 LSPosed 中管理。撤销时停用或卸载模块，然后完全结束并重新打开支付宝。

v0.1.0 是已进行真机验证的实验版本，使用调试签名；后续正式签名版本可能需要卸载旧模块后重新安装。

## 冲突原因

同一个资源 ID 在两个包中含义不同：

| ID | 支付宝聊天组件 | LineageOS 平台资源 |
| --- | --- | --- |
| `0x3f090041` | `layout/quick_menu_layout` | `string/profileNameNight`（夜晚） |
| `0x3f090019` | `layout/base_feed_layout` | `string/lineage_vendor_security_patch`（供应商安全补丁程序级别） |

运行时布局查询命中系统字符串，支付宝把文字当作 XML 文件路径打开，导致 `Resources$NotFoundException` / `FileNotFoundException`。

相关讨论：[LineageOS issue #332](https://github.com/LineageOS/issues/issues/332)。也有针对性的 [AssetManager2 系统补丁](https://github.com/WeiguangTWK/android_frameworks_base/commit/00e5c7bf68931f00a9d549e932eb4d2593981232)。本模块独立实现进程内兼容处理，并非该补丁的移植。

## 工作原理与范围

在支付宝 `BundleResources` 构造前检查传入的 `AssetManager`。只有同时包含支付宝 `libmerged-slink-bundles-res.so` 和 `org.lineageos.platform-res.apk` 时，才从该资源管理器移除后者，同步 Java/native 资源数组并使缓存失效。

- 模块包名：`local.alipay.resourcefix`，仅在支付宝主进程挂钩。
- API 33 以外跳过处理。
- 不申请 Android 权限，日志只记录模块状态和固定资源标识。
- 不修改磁盘上的系统资源包、支付宝 APK 或账户数据。
- 使用私有接口；系统或支付宝更新后需重新验证。

## 验证结果

测试日期：2026-10-15。

| 环境 | 版本 |
| --- | --- |
| 设备 | POCO F5 / marble |
| 系统 | Android 13，LineageOS 20.0-20240224-UNOFFICIAL-GMS-marble |
| 支付宝 | 10.8.50.7000 / versionCode 3801 |

- 独立资源验证：隔离前两个 ID 解析为系统字符串、XML 打开失败；隔离后恢复为支付宝布局、XML 打开成功。
- 模块启用后：两个原先闪退的个人会话正常打开，确认聊天页标题和输入框显示。
- “消息盒子”保持正常；测试中支付宝主进程 PID 不变，未发现新增崩溃。
- 未发送消息或转账；没有进行支付、指纹支付等功能的完整回归。

在 LSPosed 日志中搜索 `[AlipayResourceFix]`，可见 `Hook installed`、`Removed colliding Lineage asset`、`Verified 0x3f090041` 等状态。

## 从源码构建

需要 Python 3、JDK 17、Android SDK Build Tools（aapt2、zipalign、apksigner）。将编译依赖放在 deps：

| 文件 | 依赖 |
| --- | --- |
| `deps/android.jar` | `com.google.android:android:4.1.1.4`，仅编译接口 |
| `deps/xposed-api.jar` | `de.robv.android.xposed:api:82`，compile-only |
| `deps/r8.jar` | `com.android.tools:r8:8.3.37` |

下载位置和校验值见 [DEPENDENCIES.md](DEPENDENCIES.md)。依赖、构建产物和密钥不纳入源码仓库。

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
python build.py --aapt2 'C:\path\to\Android\Sdk\build-tools\33.0.2\aapt2.exe'
```

Linux/macOS 可提供对应平台的 JDK 和 aapt2 路径，但尚未在这些平台验证构建。

输出 `build/alipay-resource-fix-unsigned.apk`。用自己的密钥对齐和签名：

```text
zipalign -p -f 4 build/alipay-resource-fix-unsigned.apk build/alipay-resource-fix-aligned.apk
apksigner sign --ks /path/to/your.keystore --out build/alipay-resource-fix.apk build/alipay-resource-fix-aligned.apk
apksigner verify --verbose build/alipay-resource-fix.apk
```

`probe/Probe.java` 是独立资源验证程序，不打入模块，需在可访问相应资源包的调试环境中单独编译运行。

## 反馈

请提供系统版本、Android API、支付宝版本和包含 `[AlipayResourceFix]` 的模块日志。上传前去掉账号、联系人、聊天内容和设备标识。

## 许可证

源码使用 [MIT License](LICENSE)。本项目与支付宝、蚂蚁集团及 LineageOS 官方没有隶属或授权关系；第三方依赖遵循各自许可证。
