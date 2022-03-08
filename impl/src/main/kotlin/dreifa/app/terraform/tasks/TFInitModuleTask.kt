package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.fileBundle.builders.ScanDirectoryBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SKSValueType
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
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
        val output = pm.lookupOutput(processId)

        // create a new bundle with all the terraform files
        val bundleAfterInit = ScanDirectoryBuilder()
            .withId(bundle.id)
            .withName(bundle.name)
            .withBaseDirectory(location)
            .build()

        val newValue = textAdapter.fromBundle(bundleAfterInit)
        sks.put(Key.fromUniqueId(input.bundleId), newValue, SKSValueType.Text)

        //return output!!.stdout.toString() + output!!.stderr.toString()
    }
}
