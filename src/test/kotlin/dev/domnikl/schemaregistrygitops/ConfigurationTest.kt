package dev.domnikl.schemaregistrygitops

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.util.Properties

class ConfigurationTest {
    @Test
    fun `can load properties`() {
        val properties = Properties().also { it["schema.registry.url"] = "foo" }
        val config = Configuration.from(properties)

        assertEquals(mapOf("schema.registry.url" to "foo"), config.toMap())
    }

    @Test
    fun `can load properties with integer values`() {
        val properties = Properties().also { it["schema.registry.url"] = 1 }
        val config = Configuration.from(properties)

        assertEquals(mapOf("schema.registry.url" to "1"), config.toMap())
    }

    @Test
    fun `throws exception when baseUrl has not been set`() {
        assertThrows<IllegalArgumentException> {
            Configuration.from(Properties())
        }
    }

    @Test
    fun `can load from cli`() {
        val cli = mockk<CLI>()

        every { cli.propertiesFilePath } returns fromResources("client.properties").path
        every { cli.baseUrl } returns null

        val configuration = Configuration.from(cli)

        assertEquals(mapOf("schema.registry.url" to "foo"), configuration.toMap())
    }

    @Test
    fun `can load from cli and overwrite baseUrl from CLI`() {
        val cli = mockk<CLI>()

        every { cli.propertiesFilePath } returns fromResources("client.properties").path
        every { cli.baseUrl } returns "bar"

        val configuration = Configuration.from(cli)

        assertEquals(mapOf("schema.registry.url" to "bar"), configuration.toMap())
    }

    @Test
    fun `allow not using properties at all`() {
        val cli = mockk<CLI>()

        every { cli.propertiesFilePath } returns null
        every { cli.baseUrl } returns "foo"

        val configuration = Configuration.from(cli)

        assertEquals(mapOf("schema.registry.url" to "foo"), configuration.toMap())
    }

    @Test
    fun `throws exception if neither properties nor baseUrl are being set`() {
        val cli = mockk<CLI>()

        every { cli.propertiesFilePath } returns null
        every { cli.baseUrl } returns null

        assertThrows<IllegalArgumentException> {
            Configuration.from(cli)
        }
    }

    @Test
    fun `configuration from env will overwrite properties file and CLI arguments`() {
        val cli = mockk<CLI>()

        every { cli.propertiesFilePath } returns fromResources("client.properties").path
        every { cli.baseUrl } returns null

        val configuration = Configuration.from(
            cli,
            mapOf(
                "SCHEMA_REGISTRY_GITOPS_SCHEMA_REGISTRY_URL" to "foobar",
                "SCHEMA_FOO" to "should-be-ignored"
            )
        )

        assertEquals(mapOf("schema.registry.url" to "foobar"), configuration.toMap())
    }
}
