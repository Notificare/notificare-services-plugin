package re.notifica.gradle

import kotlin.test.Test
import kotlin.test.assertContains

class NotificareServicesPluginTest {

    @Test
    fun `search locations - no flavor`() {
        val locations = NotificareServicesPlugin.getJsonLocations("release", emptyList())
        assertContains(locations, "src/release/notificare-services.json")
    }

    @Test
    fun `search locations - single flavor`() {
        val locations = NotificareServicesPlugin.getJsonLocations("release", listOf("foo"))
        val expected = listOf(
            "src/fooRelease/notificare-services.json",
            "src/foo/release/notificare-services.json",
            "src/foo/notificare-services.json",
            "src/release/foo/notificare-services.json",
            "src/release/notificare-services.json",
            "notificare-services.json",
        )

        for (value in expected) {
            assertContains(locations, value)
        }
    }

    @Test
    fun `search locations - multiple flavors`() {
        val locations = NotificareServicesPlugin.getJsonLocations("release", listOf("foo", "bar"))
        val expected = listOf(
            "src/foo/bar/release/notificare-services.json",
            "src/fooBar/release/notificare-services.json",
            "src/release/fooBar/notificare-services.json",
            "src/foo/release/notificare-services.json",
            "src/foo/bar/notificare-services.json",
            "src/foo/barRelease/notificare-services.json",
            "src/release/notificare-services.json",
            "src/fooBar/notificare-services.json",
            "src/fooBarRelease/notificare-services.json",
            "src/foo/notificare-services.json",
            "src/fooRelease/notificare-services.json",
            "notificare-services.json"
        )

        for (value in expected) {
            assertContains(locations, value)
        }
    }
}
