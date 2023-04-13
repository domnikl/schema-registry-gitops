package dev.domnikl.schemaregistrygitops

import ch.qos.logback.classic.Level
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import ch.qos.logback.classic.Logger as LogbackClassicLogger

class CLITest {
    private val out = ByteArrayOutputStream()
    private val cli = CLI()
    private val commandLine = CommandLine(cli).also {
        it.out = PrintWriter(out)
    }

    @BeforeEach
    fun setupStreams() {
        out.reset()
    }

    @Test
    fun `can print version information`() {
        val exitCode = commandLine.execute("--version")

        assertEquals("schema-registry-gitops test", out.toString().trim())
        assertEquals(0, exitCode)
    }

    @Test
    fun `can enable verbose logging`() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackClassicLogger
        assertEquals(Level.INFO, logger.level)

        val exitCode = commandLine.execute("--verbose")

        assertEquals(Level.DEBUG, logger.level)
        assertEquals(0, exitCode)

        logger.level = Level.INFO
    }

    @Test
    fun `can get version`() {
        assertEquals("schema-registry-gitops test", cli.version.joinToString(""))
    }
}
