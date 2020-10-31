plugins {
    base
    id("com.diffplug.spotless") version "5.7.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    id("net.researchgate.release") version "2.8.1"
}

val encoding: String by project
val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

allprojects {
    apply(plugin = "com.diffplug.spotless")

    tasks {
        named("build") {
            dependsOn(spotlessApply)
        }
    }

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "de.marcphilipp.nexus-publish")

    tasks {
        named<Test>("test") {
            maxHeapSize = "1g"
            useJUnitPlatform()
        }

        named("build") {
            dependsOn("publishToMavenLocal")
        }

        withType<JavaCompile>().configureEach {
            options.encoding = encoding
        }

        withType<Sign>().configureEach {
            onlyIf { isReleaseVersion }
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.0")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withJavadocJar()
        withSourcesJar()
    }

    configure<de.marcphilipp.gradle.nexus.NexusPublishExtension> {
        repositories {
            sonatype()
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    val projectUrl: String by project
                    name.set(project.name)
                    description.set("Sandbox")
                    url.set(projectUrl)
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("nakamura-to")
                            name.set("Toshihiro Nakamura")
                            email.set("toshihiro.nakamura@gmail.com")
                        }
                    }
                    scm {
                        val githubUrl: String by project
                        connection.set("scm:git:${githubUrl}")
                        developerConnection.set("scm:git:${githubUrl}")
                        url.set(projectUrl)
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        val publishing = convention.findByType(PublishingExtension::class)!!
        sign(publishing.publications)
        isRequired = isReleaseVersion
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.7")
        }
        kotlin {
            ktlint("0.38.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

rootProject.apply {
    tasks {
        named("afterReleaseBuild") {
            dependsOn("publish", "closeAndReleaseRepository")
        }

        named("closeRepository") {
            onlyIf { isReleaseVersion }
        }

        named("releaseRepository") {
            onlyIf { isReleaseVersion }
        }
    }

    configure<net.researchgate.release.ReleaseExtension> {
        fun net.researchgate.release.ReleaseExtension.git(configureFn : net.researchgate.release.GitAdapter.GitConfig.() -> Unit) {
            (propertyMissing("git") as net.researchgate.release.GitAdapter.GitConfig).configureFn()
        }

        newVersionCommitMessage = "[Gradle Release Plugin] - [skip ci] new version commit: "

        git {
            requireBranch = "main"
        }
    }

    configure<io.codearte.gradle.nexus.NexusStagingExtension> {
        val sonatypeUsername: String by project
        val sonatypePassword: String by project
        username = sonatypeUsername
        password = sonatypePassword
        packageGroup = "org.seasar"
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        format("misc") {
            target("**/*.gradle.kts", "**/*.gitignore")
            targetExclude("**/bin/**", "**/build/**")
            indentWithSpaces()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
