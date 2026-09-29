plugins {
    java
    checkstyle
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val paperVersion = "26.2"
val hikariVersion = "7.1.0"
val sqliteVersion = "3.53.4.0"
val mariadbVersion = "3.5.10"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion.build.+")
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        isTransitive = false
    }

    testImplementation("io.papermc.paper:paper-api:$paperVersion.build.+")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation("com.zaxxer:HikariCP:$hikariVersion")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:$sqliteVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

checkstyle {
    toolVersion = "14.3.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "apiVersion" to paperVersion,
            "hikariVersion" to hikariVersion,
            "sqliteVersion" to sqliteVersion,
            "mariadbVersion" to mariadbVersion,
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveClassifier = ""
    }

    runServer {
        minecraftVersion(paperVersion)
    }
}
