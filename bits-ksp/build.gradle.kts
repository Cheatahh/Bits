plugins {
    kotlin("jvm") version "2.4.0"
}

group = "cheatahh.bits"
version = "1.0-release"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
    implementation(project(":bits-lib"))
}