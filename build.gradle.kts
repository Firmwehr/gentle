plugins {
	application
	id("com.google.cloud.tools.jib") version "3.1.4"
}

group = "com.github.firmwehr"
version = "SNAPSHOT"

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://jitpack.io")
	maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

application {
	mainClass.set("com.github.firmwehr.gentle.GentleCompiler")
	applicationDefaultJvmArgs = listOf("--enable-preview", "-Dspeedcenter.true")
}

buildDir = File("_build")

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

sourceSets {
	create("integrationTest") {
		java {
			srcDir("src/test/integrationTest")
		}

		compileClasspath += sourceSets.main.get().output
		runtimeClasspath += sourceSets.main.get().output
	}
}

val integrationTestImplementation: Configuration by configurations.getting {
	extendsFrom(configurations.implementation.get())
	extendsFrom(configurations.testImplementation.get())
	extendsFrom(configurations.testRuntimeOnly.get())
}

val integrationTest = task<Test>("integrationTest") {
	description = "Runs integration tests."
	group = "verification"

	testClassesDirs = sourceSets["integrationTest"].output.classesDirs
	classpath = sourceSets["integrationTest"].runtimeClasspath
	shouldRunAfter("test")
}

jib {
	from.image = "eclipse-temurin:17"
	to {
		image = "ghcr.io/firmwehr/gentle:latest"

		// here be dragons
		System.getenv("IMAGE_TAGS")?.apply {
			tags = split("[,\\n]".toRegex())
				.map { it.split(":")[1] }
				.toCollection(mutableSetOf())
		}

		container {
			environment = mapOf("GENTLE_ENABLE_LOG" to "true")
		}
	}
}

dependencies {

	// logging
	implementation("org.fusesource.jansi:jansi:2.4.0")

	// annotations
	compileOnly("org.jetbrains", "annotations", "22.0.0")

	// commons stuff
	implementation("com.google.guava:guava:31.0.1-jre")
	implementation("commons-io:commons-io:2.11.0")

	// Command line parsing
	val jbock = "5.12"
	implementation("io.github.jbock-java:jbock:$jbock")
	annotationProcessor("io.github.jbock-java:jbock-compiler:$jbock")

	implementation("com.github.firmwehr:FiAscii:e490ffd934")
	annotationProcessor("com.github.firmwehr:FiAscii:e490ffd934")

	// Persistent collections
	implementation("org.javimmutable:javimmutable-collections:3.2.1")
	// https://mvnrepository.com/artifact/net.java.dev.jna/jna
	implementation("net.java.dev.jna:jna:4.5.2")

	implementation("com.github.Firmwehr:jFirm:62c1a55f72")

	implementation("org.buildobjects:jproc:2.6.2")

	// junit
	val junitVersion = "5.8.1"
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
	testImplementation("org.assertj:assertj-core:3.21.0")
}

// set encoding for all compilation passes
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("--enable-preview")
}

tasks.getByName<Test>("test") {
	useJUnitPlatform()
	jvmArgs("--enable-preview")
}

tasks.getByName<Test>("integrationTest") {
	useJUnitPlatform()
	jvmArgs("--enable-preview")
}
