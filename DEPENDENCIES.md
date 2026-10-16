# 编译依赖

依赖仅用于本地构建，不随源码发布。Xposed API 只放在编译和 D8 classpath 上，未打入模块。

| 文件 | 版本 | SHA-256 |
| --- | --- | --- |
| android.jar | com.google.android:android:4.1.1.4 | 84072541cbb711eff89f7277100ff854929a446dba7ceb1b195c340e0b4fd3cb |
| xposed-api.jar | de.robv.android.xposed:api:82 | f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25 |
| r8.jar | com.android.tools:r8:8.3.37 | 59753e70a74f918389cc87f1b7d66b5c0862932559167425708ded159e3de439 |

下载位置：

- Android stub：[Maven Central](https://repo.maven.apache.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar)
- Xposed API：[Xposed Maven 仓库](https://api.xposed.info/repo/)，本次构建使用阿里云 Maven 镜像中的上述坐标。
- D8/R8：[Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.3.37/r8-8.3.37.jar)
- aapt2、zipalign、apksigner：Android SDK Build Tools。
