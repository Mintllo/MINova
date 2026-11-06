pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ffmpeg-kit 官方仓库 2025 归档后，com.arthenica:* 制品已从 Maven Central 主站下线
        // （repo1.maven.org/maven2/com/arthenica/... 返回 404）。阿里云 public 仓库是
        // Central 的完整镜像且仍保留该制品，作为兜底仓库放最后。
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "MINova"

include(
  ":app",
  
)
