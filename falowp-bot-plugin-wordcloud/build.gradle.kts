dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    implementation(project(":falowp-bot-utils-db"))
    //分词器
    implementation("com.hankcs:hanlp:portable-1.8.6") {
        exclude("junit", "junit")
    }
}