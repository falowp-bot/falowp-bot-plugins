dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.ktor:ktor-client-mock-jvm:3.6.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.6.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
