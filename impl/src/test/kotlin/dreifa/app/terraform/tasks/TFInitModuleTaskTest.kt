package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SKSValueType
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test
import java.io.File

class TFInitModuleTaskTest : BaseTestCase() {

    @Test
    fun `should init module`() {
        val (reg, _, sks) = buildRegistry()
        val (ctx, _) = buildExecutionContext()
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
        val initRequest = TFInitModuleRequest(moduleId)
        val initResult = TFInitModuleTaskImpl(reg).exec(ctx, initRequest)
        println(initResult)
    }

    private fun runPriorTasks(reg: Registry, ctx: ExecutionContext): UniqueId {
        val moduleId = UniqueId.randomUUID()
        val module = TFRegisterModuleParams(moduleId = moduleId, moduleName = "test")
        TFRegisterModuleTaskImpl(reg).exec(ctx, module)
        return moduleId
    }

}