dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    implementation(project(":falowp-bot-utils-db"))
    //分词器
    implementation("com.hankcs:hanlp:portable-1.8.5") {
        exclude("junit", "junit")
    }
}