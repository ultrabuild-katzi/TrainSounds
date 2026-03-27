import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("fabric-loom") version "1.15-SNAPSHOT"
    id("maven-publish")
}

val modVersion = property("mod_version") as String
val mavenGroup = property("maven_group") as String
val archivesBaseName = property("archives_base_name") as String
val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val loaderVersion = property("loader_version") as String
val fabricVersion = property("fabric_version") as String

version = modVersion
group = mavenGroup

base {
    archivesName.set(archivesBaseName)
}


fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    maven { url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")}
    maven { // Ponder, Flywheel//
        name= "createmod maven"
        url= uri("https://maven.createmod.net/")
    }
    maven("https://modmaven.dev/") // flywheel fabric

    maven { name = "Curseforge Maven"; description = "Forge Config API Port"; url = uri("https://cursemaven.com/")}
    maven {  // Flywheel, Registrate, Create
        url = uri("https://maven.tterrag.com/")
        content {
            includeGroup("com.simibubi.create")
            includeGroup("com.tterrag.registrate")
            includeGroup("com.jozufozu.flywheel")
        }
    }

    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/")
    }
    maven {
        name = "Ladysnake Libs"
        url = uri("https://maven.ladysnake.org/releases")
    }

    maven {
        name = "Fuzs Mod Resources"
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    }
    maven { url = uri("https://mvn.devos.one/snapshots/")} // Create Fabric, Porting Lib, Forge Tags, Milk Lib, Registrate Fabric
    maven { url = uri("https://mvn.devos.one/releases/")} // Porting Lib

    maven { name = "JamiesWhiteShirt Maven"; description = "Reach Entity Attributes"; url = uri("https://maven.jamieswhiteshirt.com/libs-release")}


    maven { name = "Jitpack maven"; description = "Mixin Extras & Fabric ASM"; url = uri("https://jitpack.io/") } //NOTE: LEAVE THIS AS LAST
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("com.simibubi.create:create-fabric-${project.property("minecraft_version")}:${project.property("create_fabric_version")}+mc${project.property("minecraft_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    modImplementation(files("lib/pantographsandwires-fabric-1.20.1-beta-0.1.1.jar"))
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion
        )
    }
}

val targetJavaVersion = 17
tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    from("LICENSE") {
        rename { "${it}_$archivesBaseName" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archivesBaseName
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
