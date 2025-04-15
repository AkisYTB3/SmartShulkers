plugins {
    kotlin("jvm") version "2.2.0-Beta1"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("org.ajoberstar.grgit") version "5.2.0"
}

fun getGitCommitHash(): String {
    return try {
        grgit.head().abbreviatedId ?: "NULL"
    } catch (e: Exception) {
        "NULL"
    }
}

val baseVersion = property("version") as String
version = if (baseVersion.endsWith("-dev")) {
    "$baseVersion-${getGitCommitHash()}"
} else {
    baseVersion
}

group = property("group") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("net.kyori:adventure-text-minimessage:4.19.0")
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }

    shadowJar {
        relocate("co.aikar.commands", "org.notionsmp.dreiMotd.libs.acf")
        relocate("co.aikar.locales", "org.notionsmp.dreiMotd.libs.locales")

        exclude("org.projectlombok:lombok:.*")
        exclude("net.kyori:.*")
        exclude("META-INF/**")

        minimize()

        archiveClassifier.set("")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
