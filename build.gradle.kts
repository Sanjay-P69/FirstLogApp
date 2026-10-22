plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.diffplug.spotless") version "8.0.0"
}

spotless {
    // Enforce formatting when a Kotlin source file is touched, without rewriting legacy code.
    ratchetFrom("4b699abb1a2271335578a3c736691d6c3b50c073")
    kotlin {
        target("app/src/**/*.kt")
        ktlint("1.7.1")
    }
}

val detektCli by configurations.creating

dependencies {
    detektCli("io.gitlab.arturbosch.detekt:detekt-cli:1.23.8:all")
}

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Run focused static analysis without coupling to Android/Kotlin Gradle plugins."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    val sources = fileTree("app/src") { include("**/*.kt") }
    inputs.files(sources)
    inputs.file("config/detekt/detekt.yml")
    outputs.dir(layout.buildDirectory.dir("reports/detekt"))
    args("--input", "app/src", "--config", "config/detekt/detekt.yml",
        "--report", "html:build/reports/detekt/detekt.html",
        "--report", "sarif:build/reports/detekt/detekt.sarif")
}
