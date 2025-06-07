plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    api(platform(project(":dependencies")))
    ksp(platform(project(":dependencies")))
    api(project(":domain"))
    api("me.ahoo.wow:wow-spring")
    api("io.github.oshai:kotlin-logging-jvm")
    api("me.ahoo.wow:wow-compensation-api")
    api("me.ahoo.wow:wow-compensation-core")
    ksp("me.ahoo.wow:wow-compiler")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-webflux")

//    spring security
//        implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // cosec

    api("me.ahoo.cosec:cosec-webflux")
    api("me.ahoo.cosec:cosec-ip2region")
    api("me.ahoo.cosec:cosec-spring-boot-starter")

    testImplementation("me.ahoo.wow:wow-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test, tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = 0.8.toBigDecimal()
            }
        }
    }
}
