package com.wxy.playerlite.ui.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorUsageTest {
    @Test
    fun featureUi_shouldNotReadDefaultPaletteOrLegacyFixedAccountAccents() {
        val sourceRoots = listOf(
            "app/src/main/java",
            "feature-discovery/src/main/java",
            "feature-player/src/main/java"
        ).map { relativePath -> projectRoot().resolve(relativePath) }
        val forbidden = listOf(
            "DefaultBrandPalettes.light",
            "DefaultBrandPalettes.dark",
            "AccountVisualStyle.accent"
        )

        val violations = sourceRoots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file ->
                val source = file.readText()
                forbidden.filter(source::contains).map { token ->
                    "${file.relativeTo(projectRoot()).path}: $token"
                }
            }

        assertTrue("Theme color contract violations: $violations", violations.isEmpty())
    }

    @Test
    fun recentAndMiniPlayer_shouldNotReintroduceKnownThemeLeaks() {
        val recentSource = projectRoot()
            .resolve("app/src/main/java/com/wxy/playerlite/feature/main/RecentSongsActivity.kt")
            .readText()
        val miniPlayerSource = projectRoot()
            .resolve("app/src/main/java/com/wxy/playerlite/feature/player/ui/SharedMiniPlayerBar.kt")
            .readText()

        assertFalse(recentSource.contains("isSystemInDarkTheme"))
        assertFalse(miniPlayerSource.contains("Color.White.copy(alpha = 0.995f)"))
    }

    private fun projectRoot(): File {
        val userDirectory = requireNotNull(System.getProperty("user.dir"))
        var current = File(userDirectory).absoluteFile
        repeat(8) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            current = current.parentFile ?: return@repeat
        }
        error("Unable to locate project root from $userDirectory")
    }
}
