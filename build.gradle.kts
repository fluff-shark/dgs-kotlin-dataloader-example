import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "3.2.1"
  id("io.spring.dependency-management") version "1.1.4"
  kotlin("jvm") version "1.9.21"
  kotlin("plugin.spring") version "1.9.21"
}

group = "me.fluffshark"
version = "0.0.1"

java {
  sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:6.0.5")
  }
}

dependencies {
  implementation("com.apollographql.federation:federation-graphql-java-support")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.springframework.boot:spring-boot-starter-web")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs += "-Xjsr305=strict"
    jvmTarget = "17"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
