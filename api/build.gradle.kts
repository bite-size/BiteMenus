plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "gg.bitesize"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}

java {
    withSourcesJar()
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

tasks.compileJava {
    options.release.set(25)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "bitemenus-api"
            from(components["java"])
        }
    }
}
