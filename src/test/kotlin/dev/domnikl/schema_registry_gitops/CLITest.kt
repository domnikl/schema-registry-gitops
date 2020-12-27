package dev.domnikl.schema_registry_gitops

import ch.qos.logback.classic.Level
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import ch.qos.logback.classic.Logger as LogbackClassicLogger

class CLITest {
    @Rule @JvmField val exit: ExpectedSystemExit = ExpectedSystemExit.none()

    private val err = ByteArrayOutputStream()
    private val out = ByteArrayOutputStream()
    private val oldErr: PrintStream = System.err
    private val oldOut: PrintStream = System.out

    @Before
    fun setupStreams() {
        out.reset()
        err.reset()
        System.setOut(PrintStream(out))
        System.setErr(PrintStream(err))
    }

    @After
    fun restoreStreams() {
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    @Test
    fun `can print version information`() {
        CLI.commandLine(Factory()).execute("--version")

        assert(out.toString().startsWith("schema-registry-gitops"))
    }

    @Test
    fun `can enable verbose logging`() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackClassicLogger
        assertEquals(Level.WARN, logger.level)

        CLI.commandLine(Factory()).execute("--verbose")

        assertEquals(Level.DEBUG, logger.level)

        exit.expectSystemExitWithStatus(0)

        logger.level = Level.WARN
    }
}
