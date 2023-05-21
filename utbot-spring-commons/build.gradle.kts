val springBootVersion: String by rootProject
val rdVersion: String by rootProject

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

    implementation("com.jetbrains.rd:rd-core:$rdVersion")
}

