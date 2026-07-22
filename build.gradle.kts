plugins {
    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.mc.mod.publisher) apply false
}

allprojects {
    group = project.findProperty("mavenGroup")!!
}
