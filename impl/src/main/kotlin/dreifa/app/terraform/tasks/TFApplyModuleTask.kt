package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import java.util.*
import dreifa.app.types.UniqueId
import java.io.File

data class TFApplyModuleRequest(val moduleId: UniqueId, val bundleId: UniqueId)

class TFApplyModuleTask(registry: Registry) : BaseTerraformTask<TFApplyModuleRequest, String>(registry) {

    override fun exec(ctx: ExecutionContext, input: TFApplyModuleRequest): String {

        val location = location(ctx)
        recoverBundle(location, input.bundleId)

        val pb = ProcessBuilder(terraform(), "apply", "-auto-approve")
            .directory(File(File(location).absolutePath))
        val processId = UUID.randomUUID()

        val pm = ctx.processManager()
        pm.registerProcess(pb, processId, "terraform-apply")
        waitForProcess(ctx, processId)
        val output = pm.lookupOutput(processId)

        return output!!.stdout.toString()
        //return ""
    }
}
