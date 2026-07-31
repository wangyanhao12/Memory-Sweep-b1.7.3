import java.net.URI

plugins {
	id("maven-publish")
	id("fabric-loom") version "1.15.3"
	id("babric-loom-extension") version "1.15.3"
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

base.archivesName = project.properties["archives_base_name"] as String
version = project.properties["mod_version"] as String
group = project.properties["maven_group"] as String

repositories {
	maven("https://maven.glass-launcher.net/snapshots/")
	maven("https://maven.glass-launcher.net/releases/")
	maven("https://maven.glass-launcher.net/babric")
	mavenCentral()
}

dependencies {
	minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
	mappings("net.glasslauncher:biny:${project.properties["yarn_mappings"]}:v2")
	modImplementation("net.fabricmc:fabric-loader:${project.properties["loader_version"]}")

	implementation("org.apache.logging.log4j:log4j-core:2.17.2")
	implementation("org.slf4j:slf4j-api:1.8.0-beta4")
	implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")

	// StationAPI —— 本模组的前置
	// transitiveImplementation 会让 babric-loom 把这个依赖一并带入其他依赖本模组的开发环境
	modImplementation("net.modificationstation:StationAPI:${project.properties["stationapi_version"]}")
}

// 排除旧的 "babric:fabric-loader" 坐标,统一使用 "net.fabricmc:fabric-loader"
configurations.all {
	exclude("babric")
}

tasks.withType<ProcessResources> {
	inputs.property("version", project.properties["version"])

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.properties["version"]))
	}
}

// 无论系统默认编码是什么,统一使用 UTF-8,避免中文注释/字符串乱码
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

java {
	withSourcesJar()
}

tasks.withType<Jar> {
	from("LICENSE") {
		rename { "${it}_${project.properties["archives_base_name"]}" }
	}
}

tasks.withType<GenerateModuleMetadata> {
	enabled = false
}

publishing {
	publications {
		register("mavenJava", MavenPublication::class) {
			artifactId = project.properties["archives_base_name"] as String
			from(components["java"])
		}
	}
	repositories {
		mavenLocal()
	}
}
