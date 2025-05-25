import org.gradle.kotlin.dsl.repositories

rootProject.name = "natty-wow"


fileTree(rootDir) {
    include("**/*.gradle.kts")
    exclude(
        "build",
        ".idea",
        "**/gradle",
        "config",
        "document",
        ".github",
        "deploy",
        "settings.gradle.kts",
        "buildSrc",
        "/build.gradle.kts",
        "example",
        ".",
        "out"
    )
}.forEach {
    val projectPath = it.parentFile.absolutePath
        .replace(rootDir.absolutePath, "")
        .replace(File.separator, ":")
    include(projectPath)
    project(projectPath).projectDir = it.parentFile
    project(projectPath).buildFileName = it.name
    //    如果是module模块 。所有的内容加上父亲级别目录
    if (projectPath.startsWith(":modules")) {
        project(projectPath).name = it.parentFile.absolutePath
            .replace(rootDir.absolutePath, "")
            .replace(File.separator, "-").substring(1)
    }
}


pluginManagement {
    repositories {
        // 本地 Maven 仓库（调试时使用）
        mavenLocal()

        // 国内镜像源（腾讯云）
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public") }
        // 国内镜像源（阿里云）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 备用仓库
        mavenCentral()
        google()
        gradlePluginPortal()

        // JitPack（如需）
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // 禁止 build.gradle.kts 覆盖仓库
    repositories {
        // 本地 Maven 仓库（调试时使用）
        mavenLocal()

        // 国内镜像源（腾讯云）
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public") }
        // 国内镜像源（阿里云）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 备用仓库
        mavenCentral()
        google()
        gradlePluginPortal()

        // JitPack（如需）
        maven { url = uri("https://jitpack.io") }
    }
}

gradle.settingsEvaluated {
    if (JavaVersion.current() < JavaVersion.VERSION_17) {
        throw GradleException("This build requires JDK 17. It's currently ${JavaVersion.current()}.")
    }
}
