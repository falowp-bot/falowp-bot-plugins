dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    implementation(project(":falowp-bot-utils-db"))
    implementation(project(":falowp-bot-plugin-auth"))
    //二维码
    implementation("com.google.zxing:javase:3.5.3")
}