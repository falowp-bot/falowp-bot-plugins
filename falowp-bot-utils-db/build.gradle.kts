val exposedVersion = "0.60.0"

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    //数据库连接
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}