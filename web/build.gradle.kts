import org.teavm.gradle.api.OptimizationLevel

plugins {
    java
    id("com.github.xpenatan.gdx-teavm") version "1.6.1"
}

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
    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

gdxTeaVM {
    reflectionEnabled.set(false)

    js {
        mainClass.set("dev.depotreplay.web.WebLauncher")
        htmlTitle.set("DepotReplay")
        htmlWidth.set(1280)
        htmlHeight.set(800)
        optimization.set(OptimizationLevel.BALANCED)
        obfuscated.set(true)
        serverPort.set(8080)
        targetFileName.set("depotreplay.js")
    }
}

tasks.matching {
    it.name == "generateJavaScript" || it.name.startsWith("gdx_teavm_web_")
}.configureEach {
    notCompatibleWithConfigurationCache("gdx-teavm 1.6.1 keeps a non-serializable task logger")
}

val webDist = tasks.register<Sync>("webDist") {
    group = "distribution"
    description = "Builds the static browser-playable site."
    dependsOn("gdx_teavm_web_js_build")
    from(layout.buildDirectory.dir("dist/js/webapp")) {
        exclude("WEB-INF/**", "index.html")
    }
    from("src/main/webapp")
    into(layout.buildDirectory.dir("site"))
}

tasks.named("check") {
    dependsOn(webDist)
}
