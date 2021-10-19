plugins {
	application
}

group = "com.github.firmwehr"
version = "SNAPSHOT"

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://jitpack.io")
}

application {
	mainClass.set("com.github.firmwehr.gentle.GentleCompiler")
	applicationDefaultJvmArgs = listOf("--enable-preview");
}

buildDir = File("_build")

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

	// logging
	val slf4jVersion = "1.7.32"
	val log4j2 = "2.14.1"
	implementation("org.slf4j:slf4j-api:$slf4jVersion")
	implementation("org.apache.logging.log4j:log4j-core:$log4j2")
	implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2")
	implementation("com.djdch.log4j:log4j-staticshutdown:1.1.0") // https://stackoverflow.com/a/28835409/1834100

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
}

// set encoding for all compilation passes
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("--enable-preview")
}

tasks.getByName<Test>("test") {
	useJUnitPlatform()
}
