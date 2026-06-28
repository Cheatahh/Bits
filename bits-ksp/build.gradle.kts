plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
}

group = "cheatahh.bits"
version = "1.0.5"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifactId = "bits-ksp"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/cheatahh/Bits")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
    implementation(project(":bits-lib"))
}