package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TFUploadTemplatesTaskTest : BaseTestCase() {

    @Test
    fun `should upload templates`() {
        val (reg, _, sks) = buildRegistry()
        val (ctx, _) = buildExecutionContext()
        val moduleId = runPriorTasks(reg, ctx)

        // create the file bundle with the template, and store it the KV store
        val bundle = Fixtures.templateBundle()

        // upload the template
        val uploadRequest = TFUploadTemplatesRequest(moduleId, bundle)
        TFUploadTemplatesTaskImpl(reg).exec(ctx, uploadRequest)

        // there should now be any entry in the KV store
        sks.get(Key.fromUniqueId(bundle.id))
    }

    private fun runPriorTasks(reg: Registry, ctx: ExecutionContext): UniqueId {
        val moduleId = UniqueId.randomUUID()
        val module = TFRegisterModuleParams(moduleId = moduleId, moduleName = "test")
        TFRegisterModuleTaskImpl(reg).exec(ctx, module)
        return moduleId
    }

}