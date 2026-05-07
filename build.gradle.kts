plugins {
    application
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "2.3.11"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "uk.ac.leeds.comp2850"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.11"
val logbackVersion = "1.4.14"
val pebbleVersion = "3.2.2"

dependencies {
    // Ktor server core
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Pebble templating
    implementation("io.pebbletemplates:pebble:$pebbleVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // CSV handling
    implementation("org.apache.commons:commons-csv:1.10.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")

    // SQL
    implementation("org.xerial:sqlite-jdbc:3.41.2.1")

    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")

    // Java Mail/Jakarta Mail
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Stripe API
    implementation("com.stripe:stripe-java:24.16.0")

    // JSON
    implementation("org.json:json:20230227")

    // env
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    // Enable development mode for hot reload
    systemProperty("io.ktor.development", "true")
}

kotlin {
    jvmToolchain(17)
}

// Code quality: Detekt (static analysis)
// Reports violations as warnings, doesn't fail build
detekt {
    config.setFrom(files("detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = true // Report but don't fail build
}

// Code quality: ktlint (code style)
// Reports violations as warnings, doesn't fail build
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    ignoreFailures.set(true) // Report but don't fail build
}
