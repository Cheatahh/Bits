plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
}

group = "cheatahh.bits"
version = "1.0.0"

publishing {
    publications {
        create<MavenPublication>("lib") {
            from(components["java"])
            artifactId = "bits-lib"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {

}