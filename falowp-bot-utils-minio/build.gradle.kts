val minIOVersion = "8.5.13"

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    //minIO
    api("io.minio:minio:$minIOVersion")
}