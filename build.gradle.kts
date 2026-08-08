plugins {
    // billing:7.1.1's AAR declares it was built with a newer AGP; checkAarMetadata
    // hard-fails anything older, so don't go below 8.6.x.
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
