# MINova

一个 Android 音乐播放器：多平台音源聚合、歌单导入与云同步、歌词、个性化主题。

> 本项目采用 **MIT 协议**，详见 [LICENSE](LICENSE)。

---

## 功能

- **多平台音源聚合**：酷我 / 咪咕 / B站 / 网易云 / QQ音乐 / 酷狗，一次搜索聚合多个平台的结果
- **歌单导入**：支持从各音乐平台导入自己的歌单，歌单按账号隔离
- **云端同步**：登录后歌单自动同步到服务端，卸载重装 / 换设备可恢复
- **歌单播放**：导入的歌单支持顺序播放、自动续播下一首
- **歌词**：支持 LRC 解析，逐字时间戳（卡拉OK式）歌词、翻译行
- **播放器**：全屏播放页、迷你播放器、通知栏控制、MediaSession
- **下载**：歌曲下载到本地并离线播放
- **双视觉风格**：液态玻璃（liquid glass）与 Material 3 一键切换
- **个性化**：主题色、启动引导、开屏壁纸与 Banner 自定义
- **账号**：注册 / 登录 / 收藏 / 播放历史 / 反馈

## 环境要求

| 项目 | 版本 |
|---|---|
| Android Studio | Ladybug 或更新 |
| JDK | 17 |
| Android Gradle Plugin | 见 `gradle/libs.versions.toml` |
| minSdk / targetSdk | 24 / 35 |
| Kotlin | 见 `gradle/libs.versions.toml` |

## 构建

1. 用 Android Studio 打开本目录（或命令行执行下面步骤）
2. 在项目根目录创建 `local.properties`，指向本机 Android SDK：

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

   > Windows 示例：`sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk`

3. 编译：

   ```bash
   ./gradlew assembleDebug      # 调试包
   ./gradlew assembleRelease    # 发布包（需自行配置签名）
   ```

### 发布签名

发布包需要自己的签名。在 `app/build.gradle` 里配置 `signingConfigs`，
并把 keystore 放在项目内（**不要把 keystore 提交到仓库**）：

```gradle
signingConfigs {
    release {
        storeFile file("your.keystore")
        storePassword "******"
        keyAlias "******"
        keyPassword "******"
    }
}
```

## 服务端地址配置

客户端需要连接到音乐聚合服务。默认地址是占位符：

```
YOUR_API_BASE_URL        # 业务后端（账号 / 收藏 / 歌单 / 公告）
YOUR_API_BASE_URL/music-api   # 音乐聚合服务（搜索 / 播放地址 / 歌词）
```

搜索代码里的 `MusicApiPrefs`（或 `API_BASE_URL` 常量），替换为你自己的服务地址。
音乐聚合服务可自行部署（如开源的 `go-music-api` 类项目）。

## 目录结构

```
app/src/main/kotlin/com/HyperX/MI/Na/
├── MainActivity.kt              # 主界面、播放器、ViewModel、歌词解析
├── DiscoverScreen.kt            # 推荐页
├── PlaylistScreen.kt            # 歌单页、歌单导入、本地歌单存储
├── PlaylistCloudSync.kt         # 歌单云端同步
├── ProfileTab.kt                # 我的：收藏 / 历史 / 设置
├── MusicApiClient.kt            # 音乐接口客户端（搜索 / 播放地址 / 歌词）
├── AuthScreens.kt               # 登录注册
├── logs.kt                      # 启动引导
├── StyleGuidePage.kt            # 风格引导
├── Motion.kt                    # 统一动效规范
├── ui/theme/                    # 主题、配色、字体
├── liquid/                      # 液态玻璃效果
├── animation/                   # 手势与高亮动画
└── ...
```

## 已知说明

- 音乐数据均来自第三方公开接口，本项目**不存储任何音乐文件**
- 部分平台的完整播放 / 歌词需要登录对应平台账号
- 服务端地址、密钥等敏感信息在开源版本中已替换为占位符

## 协议

MIT License —— 可自由使用、修改、分发、商用，只需保留版权声明。
