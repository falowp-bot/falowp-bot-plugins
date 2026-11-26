dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    compileOnly(project(":falowp-bot-utils-db"))
    implementation("org.jetbrains.exposed:exposed-jdbc:1.6.1")
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")
    testImplementation(project(":falowp-bot-utils-db"))
    testRuntimeOnly("com.h2database:h2:2.4.240")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform { excludeTags("live") } }

// 手动执行时会向 FALOWP_QQ_LIVE_GROUP 指定的测试群发送真实消息
tasks.register<Test>("liveTest") {
    description = "Verify native QQ against an explicitly configured test group"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("live") }
    outputs.upToDateWhen { false }
}

val ui = layout.projectDirectory.dir("ui")
val npm = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"
val installUi = tasks.register<Exec>("installUi") {
    workingDir(ui)
    commandLine(npm, "ci", "--no-audit", "--no-fund")
    inputs.files(ui.file("package.json"), ui.file("package-lock.json"))
    outputs.dir(ui.dir("node_modules"))
}
val buildUi = tasks.register<Exec>("buildUi") {
    dependsOn(installUi)
    workingDir(ui)
    commandLine(npm, "run", "build")
    inputs.dir(ui.dir("src"))
    inputs.files(
        ui.file("index.html"),
        ui.file("vite.config.ts"),
        ui.file("tsconfig.json"),
        ui.file("package-lock.json")
    )
    outputs.dir(ui.dir("dist"))
}
tasks.processResources {
    dependsOn(buildUi)
    from(ui.dir("dist")) { into("qq-native/ui") }
}

val testWorker = tasks.register<Exec>("testWorker") {
    val tests = fileTree("src/test/js") { include("**/*.test.cjs") }
    inputs.files(tests)
    inputs.dir("src/main/resources/qq-native/core")
    commandLine(listOf("node", "--test") + tests.files.sortedBy { it.path }.map { it.absolutePath })
}
tasks.check { dependsOn(testWorker) }

// 源码包包含原生桥接源码，便于复现构建
tasks.named<Jar>("sourcesJar") {
    from("src/main/native") { into("native") }
}
