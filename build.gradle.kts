plugins {
    application
    kotlin("jvm") version "2.4.0"
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "gg.bitesize"
version = "1.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    implementation(project(":api"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        relocate("kotlin", "gg.bitesize.menus.libs.kotlin")
        relocate("org.bstats", "gg.bitesize.menus.libs.bstats")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("26.3")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

application {
    mainClass.set("BiteMenusKt")
}