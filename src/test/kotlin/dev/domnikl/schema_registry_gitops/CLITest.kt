package dev.domnikl.schema_registry_gitops

import ch.qos.logback.classic.Level
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import ch.qos.logback.classic.Logger as LogbackClassicLogger

class CLITest {
    private val out = ByteArrayOutputStream()
    private val oldOut: PrintStream = System.out

    @Before
    fun setupStreams() {
        out.reset()
        System.setOut(PrintStream(out))
    }

    @After
    fun restoreStreams() {
        System.setOut(oldOut)
    }

    @Test
    fun `can print version information`() {
        val exitCode = CLI.commandLine(Factory()).execute("--version")

        assertEquals("schema-registry-gitops test", out.toString().trim())
        assertEquals(0, exitCode)
    }

    @Test
    fun `can enable verbose logging`() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackClassicLogger
        assertEquals(Level.INFO, logger.level)

        val exitCode = CLI.commandLine(Factory()).execute("--verbose")

        assertEquals(Level.DEBUG, logger.level)
        assertEquals(0, exitCode)

        logger.level = Level.INFO
    }

    @Test
    fun `can get version`() {
        assertEquals("schema-registry-gitops test", CLI.version.joinToString(""))
    }
}
