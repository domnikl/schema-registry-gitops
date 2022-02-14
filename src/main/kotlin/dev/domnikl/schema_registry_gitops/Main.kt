package dev.domnikl.schema_registry_gitops

import dagger.Component
import picocli.CommandLine
import kotlin.system.exitProcess

@Component(modules = [AppModule::class])
interface AppComponent {
    fun commandLine(): CommandLine
}

fun main(args: Array<String>) {
    val appComponent = DaggerAppComponent.create()

    exitProcess(appComponent.commandLine().execute(*args))
}
