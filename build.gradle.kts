plugins {
    id("org.springframework.boot") version Versions.SPRING_BOOT apply false
    id("io.spring.dependency-management") version Versions.DEPENDENCY_MANAGEMENT apply false
}

subprojects {
    apply(plugin = "java")

    group = "com.observer"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(Versions.JAVA_VERSION)
        }
    }

    dependencies {
        "compileOnly"(Libs.LOMBOK)
        "annotationProcessor"(Libs.LOMBOK)
        "testCompileOnly"(Libs.LOMBOK)
        "testAnnotationProcessor"(Libs.LOMBOK)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
