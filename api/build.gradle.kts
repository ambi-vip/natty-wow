plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    api(platform(project(":dependencies")))
    ksp(platform(project(":dependencies")))
    api(project(":common"))
//    api(libs.casdoor)
    api(libs.swagger.annotations)
    api("me.ahoo.wow:wow-models")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("jakarta.validation:jakarta.validation-api")
    api("io.projectreactor:reactor-core")
    implementation("org.springframework:spring-web")
    ksp("me.ahoo.wow:wow-compiler")
}
