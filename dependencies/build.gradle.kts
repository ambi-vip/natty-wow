
dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(platform(libs.cosid.bom))
    api(platform(libs.wow.bom))
    api(platform(libs.coapi.bom))
    api(platform(libs.cocache.bom))
    api(platform(libs.cosec.bom))
    api(platform(libs.cosky.bom))
    api(platform(libs.simba.bom))
    api(platform(libs.springdoc.bom))
    constraints {
        api(libs.guava)
        api(libs.kotlin.logging)
        api(libs.swagger.annotations)
        api(libs.hamcrest)
        api(libs.mockk)
        api(libs.detekt.formatting)
//        api(libs.casdoor)
        api(libs.jte)
    }
}
