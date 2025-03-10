val exposedVersion = "0.57.0"

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    //数据库连接
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}