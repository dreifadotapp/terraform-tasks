package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import java.util.*
import dreifa.app.types.UniqueId
import java.io.File

data class TFInitModuleRequest(val moduleId: UniqueId, val bundleId: UniqueId)
interface TFInitModuleTask : BlockingTask<TFInitModuleRequest, Unit>, IdempotentTask

class TFInitModuleTaskImpl(registry: Registry) : BaseTerraformTask<TFInitModuleRequest, Unit>(registry),
    TFInitModuleTask {

    override fun exec(ctx: ExecutionContext, input: TFInitModuleRequest) {

        val location = location(ctx)
        val bundle = recoverBundle(location, input.bundleId)

        val pb = ProcessBuilder(terraform(), "init")
            .directory(File(File(location).absolutePath))
        val processId = UUID.randomUUID()

        val pm = ctx.processManager()
        pm.registerProcess(pb, processId, "terraform-init")
        waitForProcess(ctx, processId)

        storeUpdatedBundle(bundle, location)
    }

}
