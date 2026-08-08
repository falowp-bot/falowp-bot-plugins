plugins {
    kotlin("plugin.serialization") version "2.4.0"
}

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    implementation(project(":falowp-bot-utils-db"))
    implementation("ai.koog:koog-agents:1.1.1")
    implementation("io.github.oshai:kotlin-logging:8.0.4")
}
