val exposedVersion = "0.50.1"

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
    //数据库连接
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    api("com.mysql:mysql-connector-j:8.4.0")
    api("org.postgresql:postgresql:42.7.3")
}