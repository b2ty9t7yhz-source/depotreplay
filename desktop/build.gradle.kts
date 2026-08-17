import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    application
    java
}

val gdxVersion = "1.14.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(project(":game"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass = "dev.depotreplay.desktop.DesktopLauncher"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

val jpackageInput = layout.buildDirectory.dir("jpackage/input")
val currentOsName = System.getProperty("os.name").lowercase(Locale.ROOT)
val jpackageAppImageOutput = if (currentOsName.contains("mac")) {
    File(System.getProperty("java.io.tmpdir"), "depotreplay-jpackage-${project.version}/app-image")
} else {
    layout.buildDirectory.dir("jpackage/app-image").get().asFile
}
val jpackageInstallerOutput = layout.buildDirectory.dir("jpackage/installer")
val desktopJar = tasks.named<Jar>("jar")
val java21Launcher = extensions.getByType<JavaToolchainService>().launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}
val executableSuffix = if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("win")) ".exe" else ""
val jpackageExecutable = java21Launcher.map {
    it.metadata.installationPath.file("bin/jpackage$executableSuffix")
}

val prepareJpackageInput = tasks.register<Sync>("prepareJpackageInput") {
    group = "distribution"
    description = "Collects the application JAR and runtime dependencies for jpackage."
    dependsOn(desktopJar)
    from(desktopJar.flatMap { it.archiveFile })
    from(configurations.runtimeClasspath)
    into(jpackageInput)
}

fun jpackageArguments(type: String, destination: File): List<String> {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val semanticVersion = project.version.toString()
    // CFBundleShortVersionString must start above zero; encode 0.x.y as x.y on macOS.
    val packageVersion = if (osName.contains("mac") && semanticVersion.startsWith("0.")) {
        semanticVersion.removePrefix("0.")
    } else {
        semanticVersion
    }
    val arguments = mutableListOf(
            "--type", type,
            "--dest", destination.absolutePath,
            "--input", jpackageInput.get().asFile.absolutePath,
            "--name", "DepotReplay",
            "--main-jar", desktopJar.get().archiveFileName.get(),
            "--main-class", "dev.depotreplay.desktop.DesktopLauncher",
            "--app-version", packageVersion,
            "--vendor", "Jinhan Zhang",
            "--description", "Deterministic dispatch strategy game",
            "--copyright", "Copyright 2026 Jinhan Zhang",
            "--add-modules", "java.base,java.desktop,java.sql,jdk.unsupported",
            "--java-options", "-Ddepotreplay.packaged=true"
    )
    if (type != "app-image") {
        arguments += listOf("--license-file", rootProject.file("LICENSE").absolutePath)
    }
    when {
        osName.contains("mac") -> {
            arguments += listOf(
                    "--java-options", "-XstartOnFirstThread",
                    "--mac-package-identifier", "dev.depotreplay",
                    "--mac-package-name", "DepotReplay",
                    "--mac-app-category", "games"
            )
        }
        osName.contains("win") && type != "app-image" -> {
            arguments += listOf(
                    "--win-dir-chooser",
                    "--win-menu",
                    "--win-menu-group", "DepotReplay",
                    "--win-shortcut"
            )
        }
        osName.contains("linux") && type != "app-image" -> {
            arguments += listOf(
                    "--linux-package-name", "depotreplay",
                    "--linux-menu-group", "Game",
                    "--linux-app-category", "Game",
                    "--linux-shortcut",
                    "--linux-deb-maintainer", "Jinhan Zhang"
            )
        }
    }
    return arguments
}

val jpackageAppImage = tasks.register<Exec>("jpackageAppImage") {
    group = "distribution"
    description = "Builds a host-native application image with a bundled Java 21 runtime."
    dependsOn(prepareJpackageInput)
    inputs.dir(jpackageInput)
    outputs.dir(jpackageAppImageOutput)
    notCompatibleWithConfigurationCache("jpackage arguments depend on the current host platform")
    doFirst {
        delete(jpackageAppImageOutput)
        val executable = jpackageExecutable.get().asFile
        require(executable.isFile) { "Java 21 jpackage was not found at $executable" }
        commandLine(executable.absolutePath)
        args(jpackageArguments("app-image", jpackageAppImageOutput))
    }
}

val jpackageInstaller = tasks.register<Exec>("jpackageInstaller") {
    group = "distribution"
    description = "Builds the host installer: PKG on macOS, MSI on Windows, or DEB on Linux."
    dependsOn(prepareJpackageInput)
    inputs.dir(jpackageInput)
    outputs.dir(jpackageInstallerOutput)
    notCompatibleWithConfigurationCache("jpackage arguments depend on the current host platform")
    doFirst {
        delete(jpackageInstallerOutput)
        val executable = jpackageExecutable.get().asFile
        require(executable.isFile) { "Java 21 jpackage was not found at $executable" }
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        val packageType = when {
            osName.contains("mac") -> "pkg"
            osName.contains("win") -> "msi"
            osName.contains("linux") -> "deb"
            else -> error("Unsupported jpackage host: $osName")
        }
        commandLine(executable.absolutePath)
        args(jpackageArguments(packageType, jpackageInstallerOutput.get().asFile))
    }
    doLast {
        val artifacts = jpackageInstallerOutput.get().asFileTree.files
                .filter(File::isFile)
                .sortedBy(File::getName)
        val digest = MessageDigest.getInstance("SHA-256")
        val lines = artifacts.map { artifact ->
            val hash = artifact.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                HexFormat.of().formatHex(digest.digest())
            }
            "$hash  ${artifact.name}"
        }
        jpackageInstallerOutput.get().file("SHA256SUMS").asFile.writeText(
                lines.joinToString(separator = System.lineSeparator(), postfix = System.lineSeparator())
        )
    }
}
