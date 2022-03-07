package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.UniqueId

data class TerraformModule(val id: UniqueId, val name: String)

object ModuleCreatedEventFactory : EventFactory {
    fun create(module: TerraformModule): Event {
        return Event(
            type = eventType(),
            aggregateId = module.id.toString(),
            payload = module
        )
    }

    override fun eventType(): String = "dreifa.app.terraform.tasks.ModuleCreated"
}

class TFCreateModuleTask(registry: Registry) : BaseBlockingTask<TerraformModule, Unit>() {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: TerraformModule) {
        val ev = ModuleCreatedEventFactory.create(input)

        // not the best rule, but for now silently
        // skip if the event is already created
        if (ses.read(existingModuleQuery(input.id)).isEmpty()) {
            ses.store(ev)
            ctx.log(LogMessage.info("Created new TerraformModule of: $input"))
        } else {
            ctx.log(LogMessage.warn("TerraformModule with ${input.id} already exists"))
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

