package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.ScanDirectoryBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSValueType
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import java.io.File
import java.lang.RuntimeException
import java.util.*

abstract class BaseTerraformTask<I, O>(registry: Registry) : BlockingTask<I, O> {
    private val locations = registry.get(Locations::class.java)
    private val sks = registry.get(SKS::class.java)
    private val textAdapter = TextAdapter()

    protected fun location(ctx: ExecutionContext): String {
        return File(locations.serviceHomeDirectory(ctx, "terraform")).absolutePath
    }

    protected fun waitForProcess(
        ctx: ExecutionContext,
        processId: UUID,
        waitMs: Long = 60000
    ) {
        val pm = ctx.processManager()
        val timeout = System.currentTimeMillis() + waitMs
        while (pm.findById(processId)!!.process.isAlive && System.currentTimeMillis() < timeout) {
            Thread.sleep(1000)
        }

        val processInfo = pm.findById(processId)!!
        if (processInfo.process.isAlive) {
            val output = pm.lookupOutput(processId)!!
            println(output)
            throw RuntimeException("timed-out waiting for ${processInfo.label}")
        }
    }

    protected fun terraform(): String {
        // Could do with a nicer logic here :)
        val possibleLocations = listOf("/usr/bin/terraform", "/usr/local/bin/terraform")
        possibleLocations.forEach {
            if (File(it).exists()) return it
        }
        return "terraform"  // assume its in the path
    }

    /**
     * Recovers all state from the KV store
     */
    protected fun recoverBundle(
        location: String,
        bundleId: UniqueId
    ): FileBundle {
        val fileAdapter = FilesAdapter(location)
        val stored = sks.get(Key.fromUniqueId(bundleId))
        val bundle = textAdapter.toBundle(stored.toText())
        fileAdapter.fromBundle(bundle)
        return bundle
    }

    /**
     * Relaods bundle content from the local file system, and stores in the
     * KV store
     */
    protected fun storeUpdatedBundle(
        bundle: FileBundle,
        location: String
    ) {
        // create a new bundle with all the terraform files
        val bundleAfterInit = ScanDirectoryBuilder()
            .withId(bundle.id)
            .withName(bundle.name)
            .withBaseDirectory(location)
            .build()

        val newValue = textAdapter.fromBundle(bundleAfterInit)
        sks.put(Key.fromUniqueId(bundle.id), newValue, SKSValueType.Text)
    }

}