dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    implementation(project(":falowp-bot-utils-db"))
    api("org.telegram:telegrambots:6.9.7.1")
}