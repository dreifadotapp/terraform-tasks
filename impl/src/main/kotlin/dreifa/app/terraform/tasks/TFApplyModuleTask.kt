package dreifa.app.terraform.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.MapOfAny
import java.util.*
import dreifa.app.types.UniqueId
import java.io.File

data class TFApplyModuleRequest(
    val moduleId: UniqueId,
    val bundleId: UniqueId,
    val variables: MapOfAny = emptyMap()
)

interface TFApplyModuleTask : BlockingTask<TFApplyModuleRequest, Unit>, IdempotentTask

class TFApplyModuleTaskImpl(registry: Registry) : BaseTerraformTask<TFApplyModuleRequest, Unit>(registry),
    TFApplyModuleTask {

    override fun exec(ctx: ExecutionContext, input: TFApplyModuleRequest): Unit {
        val location = location(ctx)
        val bundle = recoverBundle(location, input.bundleId)

        // See https://www.terraform.io/language/values/variables
        if (input.variables.isNotEmpty()) {
            buildTFvars(location, input.variables)
        } else {
            clearTFVars(location)
        }

        val pb = ProcessBuilder(terraform(), "apply", "-auto-approve")
            .directory(File(File(location).absolutePath))
        val processId = UUID.randomUUID()

        val pm = ctx.processManager()
        pm.registerProcess(pb, processId, "terraform-apply")
        waitForProcess(ctx, processId)
        storeUpdatedBundle(bundle, location)
    }

    private fun clearTFVars(location: String) {
        val tfVars = File("$location/terraform.tfvars.json")
        if (tfVars.exists()) {
            tfVars.delete()
        }
    }

    private fun buildTFvars(location: String, variables: Map<String, Any?>) {
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule())
        val tfVars = File("$location/terraform.tfvars.json")
        mapper.writeValue(tfVars, variables)
    }
}
