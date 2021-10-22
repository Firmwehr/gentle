plugins {
	application
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
	applicationDefaultJvmArgs = listOf("--enable-preview");
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

tasks.check { dependsOn(integrationTest) }

dependencies {
	
	// logging
	val slf4jVersion = "1.7.32"
	val log4j2 = "2.14.1"
	implementation("org.slf4j:slf4j-api:$slf4jVersion")
	implementation("org.apache.logging.log4j:log4j-core:$log4j2")
	implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2")
	implementation("com.djdch.log4j:log4j-staticshutdown:1.1.0") // https://stackoverflow.com/a/28835409/1834100
	
	// annotations
	compileOnly("org.jetbrains", "annotations", "22.0.0")
	
	// commons stuff
	implementation("commons-io:commons-io:2.11.0")
	
	// Command line parsing
	val jbock = "5.12"
	implementation("io.github.jbock-java:jbock:$jbock")
	annotationProcessor("io.github.jbock-java:jbock-compiler:$jbock")
	
	// junit
	val junitVersion = "5.8.1"
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
	testImplementation("org.assertj:assertj-core:3.21.0")
	testImplementation("org.buildobjects:jproc:2.6.2")
}

// set encoding for all compilation passes
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("--enable-preview")
}

tasks.getByName<Test>("test") {
	useJUnitPlatform()
}

tasks.getByName<Test>("integrationTest") {
	useJUnitPlatform()
}
