package dev.vantafyn.core.integrations.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testVersionComparison_newerVersions() {
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.26", "0.9.25"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.25", "0.9.24"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.24", "0.9.23"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.23", "0.9.22"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.10.0", "0.9.22"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0", "0.9.22"))
        assertTrue(AppUpdateChecker.isNewerVersion("v0.9.23", "0.9.22"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.23-rc1", "0.9.22"))
        assertTrue(AppUpdateChecker.isNewerVersion("0.9.22.1", "0.9.22"))
    }

    @Test
    fun testVersionComparison_sameOrOlderVersions() {
        assertFalse(AppUpdateChecker.isNewerVersion("0.9.22", "0.9.22"))
        assertFalse(AppUpdateChecker.isNewerVersion("v0.9.22", "0.9.22"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.9.21", "0.9.22"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.8.0", "0.9.22"))
    }

    @Test
    fun testAssetMatching() {
        assertTrue(AppTarget.MOBILE.matchesAsset("app-mobile-release.apk"))
        assertFalse(AppTarget.MOBILE.matchesAsset("app-tv-release.apk"))
        assertFalse(AppTarget.MOBILE.matchesAsset("Vantafyn.Plugin.Companion_0.1.3_net9.zip"))

        assertTrue(AppTarget.TV.matchesAsset("app-tv-release.apk"))
        assertFalse(AppTarget.TV.matchesAsset("app-mobile-release.apk"))
        assertFalse(AppTarget.TV.matchesAsset("Vantafyn.Plugin.Companion_0.1.3_net10.zip"))
    }
}
