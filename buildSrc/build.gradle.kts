plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin)
}

repositories {
//	国内镜像源，海外CI拉取容易失败，在国内构建项目使用即可
    maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${libs.versions.spring.boot.get()}")
//	implementation("io.spring.javaformat:spring-javaformat-gradle-plugin:${libs.versions.spring.javaformat.get()}")
//	implementation("com.google.protobuf:protobuf-gradle-plugin:${libs.versions.google.protobuf.plugins.get()}")
//	implementation("com.google.gradle:osdetector-gradle-plugin:${libs.versions.google.osdetector.plugins.get()}")
}

tasks.jar {
    manifest.attributes.putIfAbsent(
        "Created-By",
        "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})"
    )
    manifest.attributes.putIfAbsent("Gradle-Version", GradleVersion.current())
}
