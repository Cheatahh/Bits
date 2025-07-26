val kotlinVersion = "2.2.0"
val kspVersion = "2.0.2"

plugins {
    val kotlinVersion = "2.2.0"
    kotlin("jvm") version kotlinVersion
}

group = "cheatahh.bits"
version = "0.3-dev"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kotlinVersion-$kspVersion")
    implementation(project(":bits"))
}