plugins {
    `java-library`
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
    api(project(":core"))
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
