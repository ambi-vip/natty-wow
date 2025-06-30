plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    api(platform(project(":dependencies")))
    ksp(platform(project(":dependencies")))
    api(project(":api"))
    api("me.ahoo.wow:wow-spring")
    api("me.ahoo.wow:wow-models")
    api("io.github.oshai:kotlin-logging-jvm")
    api("me.ahoo.wow:wow-compensation-core")
    ksp("me.ahoo.wow:wow-compiler")
    testImplementation("me.ahoo.wow:wow-test")
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
