package dev.domnikl.schema_registry_gitops

import dagger.Component
import dev.domnikl.schema_registry_gitops.state.Applier
import dev.domnikl.schema_registry_gitops.state.Diffing
import dev.domnikl.schema_registry_gitops.state.Dumper
import dev.domnikl.schema_registry_gitops.state.Persistence
import org.slf4j.Logger
import picocli.CommandLine
import kotlin.system.exitProcess

@Component(modules = [AppModule::class])
interface AppComponent {
    fun commandLine(): CommandLine
    fun configuration(): Configuration
    fun persistence(): Persistence
    fun diffing(): Diffing
    fun applier(): Applier
    fun dumper(): Dumper
    fun logger(): Logger
}

fun main(args: Array<String>) {
    val appComponent = DaggerAppComponent.create()

    exitProcess(appComponent.commandLine().execute(*args))
}
