package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.UniqueId

/**
 * The Task to create a new module. This should always be the first Task called. It creates
 * an event in the EventStore to record that the module has been registered.
 */
data class TFCreateModuleParams(val moduleId: UniqueId, val moduleName: String)
interface TFCreateModuleTask : BlockingTask<TFCreateModuleParams, Unit>, IdempotentTask

object ModuleCreatedEventFactory : EventFactory {
    fun create(params: TFCreateModuleParams): Event {
        return Event(
            type = eventType(),
            aggregateId = params.moduleId.toString(),
            payload = params
        )
    }

    override fun eventType(): String = "dreifa.app.terraform.tasks.ModuleCreated"
}

class TFCreateModuleTaskImpl(registry: Registry) : TFCreateModuleTask, BaseBlockingTask<TFCreateModuleParams, Unit>() {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: TFCreateModuleParams) {
        val ev = ModuleCreatedEventFactory.create(input)

        // not the best rule, but for now silently
        // skip if the event is already created
        if (ses.read(existingModuleQuery(input.moduleId)).isEmpty()) {
            ses.store(ev)
            ctx.log(LogMessage.info("Created new TerraformModule with moduleId: ${input.moduleId}"))
        } else {
            ctx.log(LogMessage.warn("TerraformModule with moduleId ${input.moduleId} already exists"))
        }
    }

    private fun existingModuleQuery(moduleId: UniqueId) =
        AllOfQuery(
            listOf(
                EventTypeQuery(eventType = ModuleCreatedEventFactory.eventType()),
                AggregateIdQuery(aggregateId = moduleId.toString())
            )
        )
}

