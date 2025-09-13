plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven {
        url = uri("https://repo.osgeo.org/repository/geotools-releases/")
    }
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit)
    implementation(libs.guava)
    implementation("commons-cli:commons-cli:1.9.0")
    implementation("org.geotools:gt-geojson-core:32.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "EdgeApp",
            "Implementation-Version" to version,
            "Main-Class" to "org.EdgeApp.EdgeApp"
        )
    }
}


tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("app-all")
    mergeServiceFiles()
}


tasks {
    build {
        dependsOn("shadowJar")
    }
    startScripts {
        dependsOn("shadowJar")
    }
}

// Define `appPath` globally
val appPath = file("${projectDir}").absolutePath

application {
    mainClass = "org.EdgeApp.EdgeApp"
    applicationDefaultJvmArgs = listOf("-Dapp.path=$appPath")
}
