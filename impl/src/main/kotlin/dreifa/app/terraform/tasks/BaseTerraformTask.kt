package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.sks.SKS
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import java.io.File
import java.lang.RuntimeException
import java.util.*


abstract class BaseTerraformTask<I, O>(registry: Registry) : BaseBlockingTask<I, O>() {
    protected val locations = registry.get(Locations::class.java)
    protected val sks = registry.get(SKS::class.java)
    protected val textAdapter = TextAdapter()

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

        val proccessInfo = pm.findById(processId)!!
        if (proccessInfo.process.isAlive) {
            val output = pm.lookupOutput(processId)!!
            throw RuntimeException("timed-out waiting for ${proccessInfo.label}")
        }
    }

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

}