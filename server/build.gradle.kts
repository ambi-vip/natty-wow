

plugins {
    alias(libs.plugins.ksp)
    application
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.spring)
    kotlin("kapt")
}

tasks.jar.configure {
    exclude("application.yaml", "bootstrap.yaml")
    manifest {
        attributes(
            "Implementation-Title" to application.applicationName,
            "Implementation-Version" to archiveVersion,
        )
    }
}
application {
    mainClass.set("site.weixing.natty.server.ServerKt")
    applicationDefaultJvmArgs = listOf(
        "-Xlog:gc*:file=logs/$applicationName-gc.log:time,tags:filecount=10,filesize=32M",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=data",
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.port=5555",
        "-Dspring.cloud.bootstrap.enabled=true",
        "-Dspring.cloud.bootstrap.location=config/bootstrap.yaml",
        "-Dspring.config.location=file:./config/",
    )
}

dependencies {
    implementation(platform(project(":dependencies")))
    testImplementation(platform(libs.junit.bom))
    ksp(platform(project(":dependencies")))
    kapt(platform(project(":dependencies")))
    ksp("me.ahoo.wow:wow-compiler")
    implementation("io.netty:netty-all")
    implementation(project(":domain"))
    implementation(project(":security"))



//    implementation("me.ahoo.wow:wow-kafka")
    implementation("me.ahoo.wow:wow-mongo")
    implementation("me.ahoo.wow:wow-redis")
//    implementation("me.ahoo.wow:wow-elasticsearch")
    implementation("me.ahoo.wow:wow-mock")
    implementation("me.ahoo.wow:wow-opentelemetry")
    implementation("me.ahoo.wow:wow-webflux")
    implementation("me.ahoo.wow:wow-spring-boot-starter")
    implementation("me.ahoo.coapi:coapi-spring-boot-starter")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui")
    implementation("me.ahoo.cosid:cosid-mongo")
//    implementation("me.ahoo.cosid:cosid-spring-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("me.ahoo.cosid:cosid-spring-boot-starter")

    // cosky
//    implementation("me.ahoo.cosky:cosky-spring-cloud-starter-discovery")
//    implementation("me.ahoo.cosky:cosky-spring-cloud-starter-config")

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
//    implementation("me.ahoo.cocache:cocache-spring-boot-starter")
    implementation("me.ahoo.simba:simba-spring-boot-starter")
    implementation("me.ahoo.simba:simba-spring-redis")


//    implementation("mysql:mysql-connector-java") // 使用合适的版本
//    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("me.ahoo.wow:wow-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.github.xiaoymin:knife4j-openapi3-ui:4.5.0")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
