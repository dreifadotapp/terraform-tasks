package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.providers.TPQueryTask
import dreifa.app.types.UniqueId
import java.util.*
import java.io.File

data class TFInitModuleRequest(val moduleId: UniqueId)
interface TFInitModuleTask : BlockingTask<TFInitModuleRequest, Unit>, IdempotentTask {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

class TFInitModuleTaskImpl(registry: Registry) : BaseTerraformTask<TFInitModuleRequest, Unit>(registry),
    TFInitModuleTask,
    TaskDoc<TFInitModuleRequest, Unit> {
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

    override fun description() =
        "The task to run the terraform init logic. Must have called TFRegisterModuleTask and TFRegisterFileBundleTask before"

    override fun examples(): List<TaskExample<TFInitModuleRequest, Unit>> {
        return TaskExamplesBuilder()
            .example("Initialising a registered module.")
            .input(TFInitModuleRequest(UniqueId.alphanumeric()))
            .inputDescription("The moduleId used in TFRegisterModuleTask")
            .done()
            .build()
    }

}
