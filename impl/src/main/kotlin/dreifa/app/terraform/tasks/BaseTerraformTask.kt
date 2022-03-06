package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.sks.SKS
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.executionContext.ExecutionContext
import java.io.File
import java.lang.RuntimeException
import java.util.*




abstract class BaseTerraformTask<I, O>(registry: Registry) : BaseBlockingTask<I, O>() {
    protected val locations = registry.get(Locations::class.java)
    protected val sks = registry.get(SKS::class.java)
    protected val textAdapter = TextAdapter()

    protected fun location(): String {
        return File(locations.serviceHomeDirectory("terraform", "test")).absolutePath
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
            println(output.stdout)
            println(output.stderr)
            throw RuntimeException("timed-out waiting for ${proccessInfo.label}")
        }
    }
}