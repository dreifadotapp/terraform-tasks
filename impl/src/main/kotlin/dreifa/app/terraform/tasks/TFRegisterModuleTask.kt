package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.UniqueId

/**
 * The Task to register a new module. This should always be the first Task called. It creates
 * an event in the EventStore to record that the module has been registered.
 */
data class TFRegisterModuleParams(val moduleId: UniqueId, val moduleName: String)
interface TFRegisterModuleTask : BlockingTask<TFRegisterModuleParams, Unit>, IdempotentTask

object ModuleRegisteredEventFactory : EventFactory {
    fun create(params: TFRegisterModuleParams): Event {
        return Event(
            type = eventType(),
            aggregateId = params.moduleId.toString(),
            payload = params
        )
    }

    override fun eventType(): String = "dreifa.app.terraform.tasks.ModuleRegistered"
}

class TFRegisterModuleTaskImpl(registry: Registry) : TFRegisterModuleTask, TaskDoc<TFRegisterModuleParams, Unit>,
    BlockingTask<TFRegisterModuleParams, Unit>{

    private val ses = registry.get(EventStore::class.java)
    private val query = TFQuery(registry)

    override fun exec(ctx: ExecutionContext, input: TFRegisterModuleParams) {
        val ev = ModuleRegisteredEventFactory.create(input)

        // not the best rule, but for now silently
        // skip if the event is already created
        if (!query.moduleRegistered(input.moduleId)) {
            ses.store(ev)
            ctx.log(LogMessage.info("Created new TerraformModule with moduleId: ${input.moduleId}"))
        } else {
            ctx.log(LogMessage.warn("TerraformModule with moduleId ${input.moduleId} already exists"))
        }
    }

    override fun description(): String = "The first Task to call. This simply registers a new module"

    override fun examples(): List<TaskExample<TFRegisterModuleParams, Unit>> {
        return TaskExamplesBuilder()
            .example("Registering a new module")
            .input(TFRegisterModuleParams(UniqueId.alphanumeric(), "Demo Module"))
            .done()
            .build()
    }
}
