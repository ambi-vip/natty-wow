rootProject.name = "natty-wow"
include(":dependencies")
include(":api")
include(":domain")
include(":server")
include(":code-coverage-report")
include(":bom")


gradle.settingsEvaluated {
    if (JavaVersion.current() < JavaVersion.VERSION_17) {
        throw GradleException("This build requires JDK 17. It's currently ${JavaVersion.current()}.")
    }
}
