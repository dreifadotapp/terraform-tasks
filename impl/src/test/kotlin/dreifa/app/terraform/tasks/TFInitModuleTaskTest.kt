package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSValueType
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.InMemoryLogging
import dreifa.app.tasks.logging.LoggingReaderContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test
import java.io.File

class TFInitModuleTaskTest {

    @Test
    fun `should init module`() {
        val (reg, ses, sks) = buildRegistry()
        val (ctx, logging) = buildExecutionContext()
        val moduleId = runPriorTasks(reg, ctx)

        // create the file bundle with the template, and store it the KV store
        val bundleId = UniqueId.randomUUID()
        val bundle = FileBundleBuilder()
            .withId(bundleId)
            .withName("Terraform Bundle")
            .addItem(TextBundleItem("main.tf", File("src/test/resources/localfile/main.tf").readText()))
            .build()
        sks.put(
            Key.fromUniqueId(bundleId),
            TextAdapter().fromBundle(bundle),
            SKSValueType.Text
        )

        // run the 'terraform init' command
        val initRequest = TFInitModuleRequest(moduleId, bundleId)
        val initResult = TFInitModuleTaskImpl(reg).exec(ctx, initRequest)
        println(initResult)
    }

    private fun runPriorTasks(reg: Registry, ctx: ExecutionContext): UniqueId {
        val moduleId = UniqueId.randomUUID()
        val module = TFCreateModuleParams(moduleId = moduleId, moduleName = "test")
        TFCreateModuleTaskImpl(reg).exec(ctx, module)
        return moduleId
    }

    private fun buildRegistry(): Triple<Registry, EventStore, SKS> {
        val ses = InMemoryEventStore()
        val sks = SimpleKVStore()
        val reg = Registry().store(ses).store(sks).store(TestLocations(baseDir = ".."))
        return Triple(reg, ses, sks)
    }

    private fun buildExecutionContext(): Pair<ExecutionContext, LoggingReaderContext> {
        val logging = InMemoryLogging()
        val ctx = SimpleExecutionContext().withInMemoryLogging(logging).withInstanceQualifier("testing")
        return Pair(ctx, logging)
    }
}