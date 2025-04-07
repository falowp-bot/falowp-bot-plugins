dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
    //腾讯云翻译api
    api("com.tencentcloudapi:tencentcloud-sdk-java-tmt:3.1.1195") {
        exclude("com.squareup.okhttp3", "okhttp")
    }
}