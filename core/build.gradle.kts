import com.android.build.gradle.*
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
    id("signing")
    id("maven-publish")
}

group = Maven.groupId
version = Versions.name

android {
    buildToolsVersion(Versions.buildTools)
    compileSdkVersion(Versions.compileSdk)

    defaultConfig {
        minSdkVersion(Versions.minSdk)
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    lintOptions {
        textReport = true
        textOutput("stdout")
    }

    libraryVariants.all {
        generateBuildConfigProvider?.configure {
            enabled = false
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = false
            isJniDebuggable = false
        }
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
        }
    }

    externalNativeBuild {
        ndkBuild {
            path(File("${projectDir}/Android.mk"))
        }
    }
    ndkVersion = Versions.ndk
}

dependencies {
    implementation(kotlin("stdlib", Versions.kotlin))
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets.create("main").allSource)
}

val customDokkaTask by tasks.creating(DokkaTask::class) {
    dokkaSourceSets.getByName("main") {
        noAndroidSdkLink.set(false)
    }
    dependencies {
        plugins("org.jetbrains.dokka:javadoc-plugin:${Versions.dokkaPlugin}")
    }
    inputs.dir("src/main/java")
    outputDirectory.set(buildDir.resolve("javadoc"))
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(customDokkaTask)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles JavaDoc JAR"
    archiveClassifier.set("javadoc")
    from(customDokkaTask.outputDirectory)
}

val publicationName = "core"
publishing {
    publications {
        create<MavenPublication>(publicationName) {
            groupId = Maven.groupId
            artifactId = Maven.artifactId
            version = Versions.name

            val releaseAar = "$buildDir/outputs/aar/${project.name}-release.aar"

            println("""
                    |Creating maven publication '$publicationName'
                    |    Group: $groupId
                    |    Artifact: $artifactId
                    |    Version: $version
                    |    Aar: $releaseAar
                """.trimMargin())

            artifact(releaseAar)
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set(Maven.name)
                description.set(Maven.desc)
                url.set(Maven.siteUrl)

                scm {
                    val scmUrl = "scm:git:${Maven.gitUrl}"
                    connection.set(scmUrl)
                    developerConnection.set(scmUrl)
                    url.set(this@pom.url)
                    tag.set("HEAD")
                }

                developers {
                    developer {
                        id.set("crow-misia")
                        name.set("Zenichi Amano")
                        email.set("crow.misia@gmail.com")
                        roles.set(listOf("Project-Administrator", "Developer"))
                        timezone.set("+9")
                    }
                }

                licenses {
                    license {
                        name.set(Maven.licenseName)
                        url.set(Maven.licenseUrl)
                        distribution.set(Maven.licenseDist)
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = URI("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (Versions.name.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            val sonatypeUsername: String by project
            val sonatypePassword: String by project
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName(publicationName))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
        }
    }
}
