dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    //分词器
    implementation("com.hankcs:hanlp:portable-1.8.4") {
        exclude("junit", "junit")
    }
}