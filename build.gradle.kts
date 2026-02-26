val kotlinVersion: String by project
val falowpBotVersion: String by project

plugins {
    kotlin("jvm") version "2.3.10"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("maven-publish")
    signing
}


group = "com.blr19c.falowp"
version = falowpBotVersion

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${rootProject.version}")
}

subprojects {
    apply(plugin = "kotlin")

    kotlin {
        version = kotlinVersion
        jvmToolchain(25)
    }

    apply(plugin = "maven-publish")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "signing")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        mavenLocal()
    }

    tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(tasks.getByName("javadoc"))
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    artifacts {
        add("archives", tasks.named<Jar>("javadocJar"))
        add("archives", tasks.named<Jar>("sourcesJar"))
    }


    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()
        coordinates(
            groupId = project.group.toString(),
            artifactId = project.name,
            version = project.version.toString()
        )
        pom {
            name.set("${project.group}:${project.name}")
            description.set("FalowpBot plugin")
            url.set("https://github.com/falowp-bot")

            scm {
                url.set("https://github.com/falowp-bot")
                connection.set("scm:git:https://github.com/falowp-bot.git")
                developerConnection.set("scm:git:ssh://git@github.com/falowp-bot.git")
            }

            licenses {
                license {
                    name.set("GPL-3.0 license")
                    url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                }
            }

            developers {
                developer {
                    id.set("falowp")
                    name.set("falowp")
                    organization.set("falowp")
                    organizationUrl.set("https://falowp.blr19c.com")
                    timezone.set("+8")
                    roles.set(listOf("owner"))
                }
            }
        }
    }

    signing {
        val key = findProperty("signingInMemoryKey") as String?
        val pass = findProperty("signingInMemoryKeyPassword") as String?
        if (!key.isNullOrBlank() && !pass.isNullOrBlank()) {
            useInMemoryPgpKeys(key, pass)
        } else {
            //如果本地gpg采用homebrew安装 需要手动指定位置
            //signing.gnupg.executable=/opt/homebrew/bin/gpg
            //如果本地gpg出现无法输入密码问题 请尝试
            //brew install pinentry-mac
            //echo "pinentry-program /opt/homebrew/bin/pinentry-mac" >> ~/.gnupg/gpg-agent.conf
            //gpgconf --kill gpg-agent
            useGpgCmd()
        }
        sign(publishing.publications)
    }
}