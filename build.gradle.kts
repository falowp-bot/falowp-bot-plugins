val kotlinVersion: String by project
val falowpBotVersion: String by project

plugins {
    kotlin("jvm") version "2.1.0-RC"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0-RC"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("maven-publish")
    signing
}


group = "com.blr19c.falowp"
version = falowpBotVersion

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.blr19c.falowp:falowp-bot-system:${project.version}")
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    kotlin {
        version = kotlinVersion
        jvmToolchain(21)
    }

    apply(plugin = "maven-publish")
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


    publishing {
        repositories {
            maven {
                // https://s01.oss.sonatype.org/content/repositories/snapshots/
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username =
                        project.findProperty("s01SonatypeUserName")?.toString() ?: System.getenv("MAVEN_USERNAME")
                    password =
                        project.findProperty("s01SonatypePassword")?.toString() ?: System.getenv("MAVEN_PASSWORD")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = project.group.toString()
                version = project.version.toString()

                println("groupId: $groupId, artifactId: $artifactId, version: $version")

                artifact(tasks.getByName<Jar>("javadocJar")) {
                    classifier = "javadoc"
                }
                artifact(tasks.getByName<Jar>("sourcesJar")) {
                    classifier = "sources"
                }

                pom {
                    name.set("${project.group}:falowp-bot-system")
                    description.set("FalowpBot plugin")
                    packaging = "jar"
                    url.set("https://github.com/falowp-bot")

                    scm {
                        url.set("https://github.com/falowp-bot")
                        connection.set("https://github.com/falowp-bot")
                        developerConnection.set("https://github.com/falowp-bot")
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
                            organization {
                                name = "falowp"
                                url = "https://falowp.blr19c.com"
                            }
                            timezone.set("+8")
                            roles.add("owner")
                        }
                    }
                }
            }
        }
    }

    signing {
        if (System.getenv("GPG_PRIVATE_KEY") != null) {
            useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSPHRASE"))
        }
        sign(publishing.publications)
    }
}