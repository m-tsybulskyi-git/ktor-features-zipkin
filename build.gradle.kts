import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

val kotlinVersion: String by project
val ktorVersion: String by project
val junit5Version: String by project
val spekVersion: String by project
val assertkVersion: String by project

plugins {
    kotlin("jvm") version "1.4.32"
    id("org.jetbrains.dokka") version "1.4.30"
    signing
    `maven-publish`
}

group = "com.michaelstrasser"
version = "0.2.10"

repositories {
    mavenCentral()
    // For kotlinx-html used by Dokka
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlinVersion)

    implementation(group = "io.ktor", name = "ktor-server-core", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-client-core", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-client-core-jvm", version = ktorVersion)

    testImplementation(group = "io.ktor", name = "ktor-client-mock", version = ktorVersion)
    testImplementation(group = "io.ktor", name = "ktor-client-mock-jvm", version = ktorVersion)
    testImplementation(group = "io.ktor", name = "ktor-server-tests", version = ktorVersion)
    testImplementation(group = "io.ktor", name = "ktor-client-tests", version = ktorVersion)

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junit5Version)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junit5Version)

    testImplementation(group = "com.willowtreeapps.assertk", name = "assertk-jvm", version = assertkVersion)

    // Override the version in Spek.
    testImplementation(kotlin(module = "reflect", version = kotlinVersion))
    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = spekVersion)
    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = spekVersion)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
    dependsOn("cleanTest")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("Ktor features Zipkin")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(
                    URL("https://github.com/mjstrasser/ktor-features-zipkin/tree/main/src/main/kotlin")
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("dokkaJar") {
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/html"))
}

publishing {
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["dokkaJar"])
            pom {
                name.set("ktor-features-zipkin")
                description.set("Ktor feature for OpenZipkin tracing IDs")
                url.set("https://github.com/mjstrasser/ktor-features-zipkin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("mjstrasser")
                        name.set("Michael Strasser")
                        email.set("code@michaelstrasser.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mjstrasser/ktor-features-zipkin.git")
                    url.set("https://github.com/mjstrasser/ktor-features-zipkin")
                }
            }
        }
    }
    val ossrhUsername: String? by project
    val ossrhPassword: String? by project
    repositories {
        maven {
            name = "buildDir"
            url = uri("file://${buildDir}/repo")
        }
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["mavenKotlin"])
}
