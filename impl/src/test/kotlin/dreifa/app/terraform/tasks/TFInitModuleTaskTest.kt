package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.FBStoreTaskImpl
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TFInitModuleTaskTest : BaseTestCase() {

    @Test
    fun `should init module`() {
        val (reg, _, _) = buildRegistry()
        val (ctx, _) = buildExecutionContext()
        val moduleId = runPriorTasks(reg, ctx)

        // run the 'terraform init' command
        val initRequest = TFInitModuleRequest(moduleId)
        val initResult = TFInitModuleTaskImpl(reg).exec(ctx, initRequest)
        println(initResult)
    }

    private fun runPriorTasks(reg: Registry, ctx: ExecutionContext): UniqueId {
        val moduleId = UniqueId.randomUUID()
        val registerModuleRequest = TFRegisterModuleParams(moduleId = moduleId, moduleName = "test")
        TFRegisterModuleTaskImpl(reg).exec(ctx, registerModuleRequest)

        val bundleId = UniqueId.randomUUID()
        val bundle = Fixtures.templateBundle(bundleId)
        val asText = TextAdapter().fromBundle(bundle)
        FBStoreTaskImpl(reg).exec(ctx, asText)

        val registerBundleRequest = TFRegisterFileBundleRequest(moduleId, bundleId)
        TFRegisterFileBundleTaskImpl(reg).exec(ctx, registerBundleRequest)
        return moduleId
    }

}