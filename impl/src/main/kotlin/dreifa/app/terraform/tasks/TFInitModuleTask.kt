package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.fileBundle.builders.ScanDirectoryBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSKeyValue
import dreifa.app.sks.SKSValueType
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
import java.util.*
import dreifa.app.types.UniqueId
import java.io.File
import java.lang.RuntimeException

data class TFInitModuleRequest(val moduleId: UniqueId, val bundleId: UniqueId)

class TFInitModuleTask(registry: Registry) : BaseTerraformTask<TFInitModuleRequest, String>(registry) {

    override fun exec(ctx: ExecutionContext, input: TFInitModuleRequest): String {

        val location = location()

        val fileAdapter = FilesAdapter(location)
        val stored = sks.get(Key.fromUniqueId(input.bundleId))
        val bundle = textAdapter.toBundle(stored.toText())
        fileAdapter.fromBundle(bundle)

        val pb = ProcessBuilder("/usr/local/bin/terraform", "init")
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

        return output!!.stdout.toString() + output!!.stderr.toString()
    }
}


fun main(args: Array<String>) {
    val reg = Registry()
    val sks = SimpleKVStore()
    val location = TestLocations(baseDir = ".")
    reg.store(sks).store(location)
    println("Running TFInitModuleTask in ${location.homeDirectory()}")

    val template = File("impl/src/test/resources/main.tf").readText()

    val bundleId = UniqueId.randomUUID()
    val moduleId = UniqueId.randomUUID()
    val bundle = FileBundleBuilder()
        .withId(bundleId)
        .withName("Simple-Terraform-Demo")
        .addItem(TextBundleItem("main.tf", template))
        .build()

    val ctx = SimpleExecutionContext()
    TFUploadModuleTask(reg).exec(ctx, TFUploadModuleRequest(moduleId, bundle))
    val result = TFInitModuleTask(reg).exec(ctx, TFInitModuleRequest(moduleId, bundleId))

    println(result)
}