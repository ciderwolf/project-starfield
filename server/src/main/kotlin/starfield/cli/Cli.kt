package starfield.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

object Cli : CliktCommand(name="cli") {
    override fun run() = Unit

    fun invoke(args: Array<String>) {
        Cli.subcommands(ImportFromScryfall, ExportData(), IngestData())
            .main(args)
    }
}