# Vido

Vido 是一个完全本地运行的 Android 视频库应用。它展示系统媒体库中的视频，也能在用户明确授权的目录中识别经过改后缀的视频文件。

## 功能

- 本机视频库浏览、按文件夹查看与标题搜索
- Media3/ExoPlayer 应用内播放，支持进度恢复、倍速、横屏和播放列表
- 可选调用系统默认播放器播放本地视频
- 私密目录：通过 Storage Access Framework 仅访问用户授权的目录
- 自定义后缀扫描：使用媒体轨道探测确认文件确实包含视频，不按后缀直接误判
- 私密目录视频可选显示封面；封面仅缓存为缩略图与元数据，不复制视频原文件

## 隐私

应用不声明网络权限，数据仅保存在设备本地。

标准视频库读取 Android 13 及以上版本的 `READ_MEDIA_VIDEO` 权限，旧版本使用兼容读取权限。私密目录使用系统目录选择器获取持久只读 URI 权限，不使用 `MANAGE_EXTERNAL_STORAGE`，也不会移动、导入或上传源视频。

私密目录入口默认隐藏：在设置页连续点击版本号 7 次，确认后进入。

## 环境

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0 或兼容版本

项目已提交 Gradle Wrapper，无需本机预装 Gradle。

## 本地构建

Windows：

```powershell
./gradlew.bat assembleDebug
```

macOS / Linux：

```bash
./gradlew assembleDebug
```

输出文件：`app/build/outputs/apk/debug/app-debug.apk`。

## GitHub Actions

推送至 `main`、向 `main` 发起 Pull Request 或手动触发工作流时，GitHub Actions 会使用 JDK 17 构建 debug APK，并将其作为 `Vido-debug-apk` 构建产物保存 14 天。

仓库不包含签名密钥。CI 产物是 debug APK；发布签名 APK 需要在仓库 Secrets 中单独配置签名材料和发布流程。

## 发布

创建并推送符合语义化版本的 tag（例如 `v1.2.3`）会触发发布工作流。工作流会将去掉 `v` 的版本号写入 APK 的 `versionName`，并以 GitHub Actions 运行号生成递增的 `versionCode`，随后上传 APK 并创建 GitHub Release。

```bash
git tag -a v1.2.3 -m "v1.2.3"
git push origin v1.2.3
```

带预发布标识的 tag（例如 `v1.2.3-beta.1`）会创建预发布 Release。更新日志分类依赖 Pull Request label，约定见 [`.github/release.yml`](.github/release.yml)。
