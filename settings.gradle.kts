rootProject.name = "natty-wow"


fileTree(rootDir) {
    include("**/*.gradle.kts")
    exclude("build", ".idea", "**/gradle", "config", "document", ".github", "deploy", "settings.gradle.kts", "buildSrc", "/build.gradle.kts", ".", "out")
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




gradle.settingsEvaluated {
    if (JavaVersion.current() < JavaVersion.VERSION_17) {
        throw GradleException("This build requires JDK 17. It's currently ${JavaVersion.current()}.")
    }
}
