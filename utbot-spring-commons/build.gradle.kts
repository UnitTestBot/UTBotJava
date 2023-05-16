val springBootVersion: String by rootProject
val kotlinLoggingVersion: String by rootProject
val commonsIOVersion: String by rootProject

plugins {
    id("java")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot
    compileOnly("org.springframework.boot:spring-boot:$springBootVersion")
    compileOnly("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    compileOnly("commons-io:commons-io:$commonsIOVersion")
}

