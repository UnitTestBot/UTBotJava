plugins {
    id("org.springframework.boot") version "2.6.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id ("java")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.15.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter:2.1.15.RELEASE")
}
