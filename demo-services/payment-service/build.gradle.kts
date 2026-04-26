plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(Libs.SPRING_BOOT_WEB)
    implementation(Libs.SPRING_BOOT_VALIDATION)

    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")
    implementation(Libs.JACKSON_DATABIND)
    implementation(Libs.SPRING_BOOT_ACTUATOR)
    runtimeOnly(Libs.MICROMETER_PROMETHEUS)
}