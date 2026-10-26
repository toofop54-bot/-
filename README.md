# DeepSeek Life · Android 0.1.2

独立 AI 虚拟社交角色 Android 测试版。支持角色设定、单轮聊天、本机动态，以及约每小时尝试的主动消息通知。Android 8.0 或更新版本。

## 下载 APK

打开本仓库 [Actions](https://github.com/toofop54-bot/-/actions)，进入最新成功的 **DeepSeek Life - Build APK** 运行，下载 **DeepSeek-Life-APK**，解压后安装 **DeepSeek-Life-debug.apk**。`DeepSeek-Life-Lint` 是检查报告。

安装后填写自己的 DeepSeek API Key、模型名称与角色设定，点击保存。先发送一条消息验证连接，再按需启用自主动态。构建不需要 API Key，请勿将密钥上传到仓库。

## 构建

本仓库直接保存完整源码，不需要上传或解压任何源码 ZIP。推送代码到 main 自动构建，也可在 Actions 手动运行。环境为 Java 17、Gradle 8.10.2、AGP 8.7.3、Android SDK 35、WorkManager 2.10.1。工作流运行 assembleDebug 和 lintDebug，成功后保留 APK 14 天。

本地安装上述环境后运行：

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
```

## 使用范围

- 动态保存在手机，主动消息发送给本机使用者，不会发往其他社交平台。
- 自动任务默认关闭，开启后每轮最多两次 DeepSeek API 请求，会使用你的 API 余额；实际执行时间由 Android 的联网、省电和后台策略决定。
- 聊天为单轮，动态最多保存 100 条；卸载会清除本机数据。
- API Key 保存在应用私有存储，禁用备份，尚未用 Android Keystore 加密。聊天和角色设定会发送到 DeepSeek。
- 当前是 debug 测试包。不同构建的签名可能不同，覆盖安装可能失败；不要在未备份数据时直接卸载。
- 构建状态以 Actions 为准；编译通过不等于已在实体手机上完成测试。
