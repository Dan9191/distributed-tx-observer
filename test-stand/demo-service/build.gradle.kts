plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
