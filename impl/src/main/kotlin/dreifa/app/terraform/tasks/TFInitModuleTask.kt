package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.UniqueId
import java.util.*
import java.io.File

data class TFInitModuleRequest(val moduleId : UniqueId)
interface TFInitModuleTask : BlockingTask<TFInitModuleRequest, Unit>, IdempotentTask

class TFInitModuleTaskImpl(registry: Registry) : BaseTerraformTask<TFInitModuleRequest, Unit>(registry),
    TFInitModuleTask {
    private val query = TFQuery(registry)

    override fun exec(ctx: ExecutionContext, input: TFInitModuleRequest) {

        val bundleId = query.bundleId(input.moduleId)
        val location = location(ctx)

        val bundle = recoverBundle(location, bundleId)

        val pb = ProcessBuilder(terraform(), "init")
            .directory(File(File(location).absolutePath))
        val processId = UUID.randomUUID()

        val pm = ctx.processManager()
        pm.registerProcess(pb, processId, "terraform-init")
        waitForProcess(ctx, processId)

        storeUpdatedBundle(bundle, location)
    }

}
