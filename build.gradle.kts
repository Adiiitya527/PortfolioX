// Root build file: declares plugin versions once for the whole project.
// "apply false" means "make this plugin available, but each module decides whether to use it".
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
