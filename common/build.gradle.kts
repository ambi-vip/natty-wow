plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    api(platform(project(":dependencies")))
    ksp(platform(project(":dependencies")))
    api(project(":core"))
    api(libs.swagger.annotations)
    api("me.ahoo.wow:wow-api")
    api("me.ahoo.wow:wow-apiclient")
    api("me.ahoo.wow:wow-models")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("jakarta.validation:jakarta.validation-api")
    api("me.ahoo.coapi:coapi-api")
    api("me.ahoo.wow:wow-cocache")
    api("io.projectreactor:reactor-core")
    implementation("org.springframework:spring-web")
    ksp("me.ahoo.wow:wow-compiler")
    api("me.ahoo.wow:wow-compensation-api")
    api("me.ahoo.wow:wow-compensation-core")
}
