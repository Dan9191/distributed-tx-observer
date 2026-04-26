plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(Libs.SPRING_BOOT_WEB)
    implementation(Libs.SPRING_BOOT_DATA_JPA)
    implementation(Libs.SPRING_BOOT_VALIDATION)
    implementation(Libs.SPRING_BOOT_FLYWAY)
    runtimeOnly(Libs.FLYWAY_POSTGRESQL)
    runtimeOnly(Libs.POSTGRESQL)
    implementation(Libs.JACKSON_DATABIND)
    implementation(Libs.SPRING_BOOT_ACTUATOR)
    runtimeOnly(Libs.MICROMETER_PROMETHEUS)
}
