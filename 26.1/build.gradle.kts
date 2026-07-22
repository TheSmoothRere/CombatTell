import io.github.smootheez.LoaderType
import io.github.smootheez.curseforge.EnvironmentType

plugins {
    id("net.fabricmc.fabric-loom")
    id("io.github.smootheez.mc-mod-publisher")
}

fun prop(name: String): String {
    val value = project.findProperty(name)
        ?: error("Required property '$name' is missing from gradle.properties!")
    return value.toString()
}

val modVersion = prop("modVersion")
val modId = prop("modId")
val modName = prop("modName")
val modDescription = prop("modDescription")
val modAuthor = prop("modAuthor")
val modLicense = prop("modLicense")
val minecraftVersion = prop("minecraftVersion")
val fabricApiVersion = prop("fabricApiVersion")
val javaVersion = prop("javaVersion")

val modReleaseType = prop("modReleaseType")
val modrinthId = prop("modrinthId")
val curseforgeId = prop("curseforgeId")

val mcModVersion = "$modVersion+$minecraftVersion"
version = mcModVersion

base {
    archivesName.set(modId)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create(modId) {
            sourceSet(sourceSets.getByName("main"))
            sourceSet(sourceSets.getByName("client"))
        }
    }

    runs {
        named("client") {
            client()
            generateRunConfig.set(true)
        }
    }
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        // forRepositories(fg.repository) // Uncomment when using ForgeGradle
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation(libs.fabric.loader)

    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("maven.modrinth:pXciMYpV:RDyihKHr") // ReLib version 0.1.1+26.1
}

tasks.processResources {
    val resourceReplacements = mapOf(
        "version" to modVersion,
        "id" to modId,
        "name" to modName,
        "description" to modDescription,
        "author" to modAuthor,
        "license" to modLicense,
        "minecraftVersion" to minecraftVersion,
        "fabricApiVersion" to fabricApiVersion,
        "javaVersion" to javaVersion
    )

    inputs.properties(resourceReplacements)
    filesMatching("fabric.mod.json") {
        expand(resourceReplacements)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion.toInt())
}

tasks.jar {
    val projectName = modId
    inputs.property("projectName", projectName)

    from(rootProject.file("LICENSE")) {
        rename { "${it}-$projectName" }
    }
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

mcModPublisher {
    displayName = "$modName $mcModVersion"
    version = mcModVersion
    releaseType = modReleaseType
    changelog = rootProject.file("changelogs/v$mcModVersion.md").readText()
    files.from(tasks.named("jar"))
    gameVersions.addAll(listOf("26.1", "26.1.1", "26.1.2"))
    loaders.addAll(listOf(LoaderType.FABRIC))

    curseforge {
        token = System.getenv("CURSEFORGE_TOKEN")
        projectId = curseforgeId
        environmentType = listOf(EnvironmentType.CLIENT)

        required("fabric-api")
        required("relib")
    }

    modrinth {
        token = System.getenv("MODRINTH_TOKEN")
        projectId = modrinthId

        required("P7dR8mSH")
        required("pXciMYpV")
    }
}
