object Versions {
    const val SPRING_BOOT           = "4.0.5"
    const val DEPENDENCY_MANAGEMENT = "1.1.7"
    const val JAVA_VERSION          = 21
    const val LOMBOK                = "1.18.34"
    const val JACKSON               = "2.17.2"
}

object Libs {
    const val LOMBOK = "org.projectlombok:lombok:${Versions.LOMBOK}"

    // Spring Boot starters (версии управляются Spring BOM)
    const val SPRING_BOOT_WEB        = "org.springframework.boot:spring-boot-starter-web"
    const val SPRING_BOOT_DATA_JPA   = "org.springframework.boot:spring-boot-starter-data-jpa"
    const val SPRING_BOOT_VALIDATION = "org.springframework.boot:spring-boot-starter-validation"
    const val SPRING_BOOT_FLYWAY = "org.springframework.boot:spring-boot-starter-flyway"

    // База данных
    const val POSTGRESQL        = "org.postgresql:postgresql"
    const val FLYWAY_POSTGRESQL = "org.flywaydb:flyway-database-postgresql"

    const val JACKSON_DATABIND = "com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}"
}
