package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.Event
import dreifa.app.ses.EventFactory
import dreifa.app.ses.EventStore
import dreifa.app.sks.SKS
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.providers.TPQueryTask
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

/**
 * The Task to upload the Terraform templates. Must have called TFRegisterModuleTask before
 * hand.
 */
data class TFRegisterFileBundleRequest(val moduleId: UniqueId, val bundleId: UniqueId)
interface TFRegisterFileBundleTask : BlockingTask<TFRegisterFileBundleRequest, Unit>, IdempotentTask {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

object FileBundledRegisteredEventFactory : EventFactory {
    fun create(params: TFRegisterFileBundleRequest): Event {
        return Event(
            type = eventType(),
            aggregateId = params.moduleId.toString(),
            payload = params
        )
    }

    override fun eventType(): String = "dreifa.app.terraform.tasks.FileBundledRegistered"
}

class TFRegisterFileBundleTaskImpl(registry: Registry) : BlockingTask<TFRegisterFileBundleRequest, Unit>,
    TFRegisterFileBundleTask,
    TaskDoc<TFRegisterFileBundleRequest, Unit> {
    private val sks = registry.get(SKS::class.java)
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: TFRegisterFileBundleRequest) {
        // persist bundle in the key-value store

        val key = Key.fromUniqueId(input.bundleId)
        if (!sks.exists(key)) {
            throw RuntimeException("Cannot find a FileBundle with ${input.bundleId} in the Simple KV Store.")
        }

        // record with event
        ses.store(FileBundledRegisteredEventFactory.create(input))
    }

    override fun description(): String = "The Task to upload the Terraform templates. Must have called " +
            "TFRegisterModuleTask beforehand AND uplaod the FileBundle with terraform module bu calling FBStoreTask"

    override fun examples(): List<TaskExample<TFRegisterFileBundleRequest, Unit>> {
        return TaskExamplesBuilder()
            .example("Upload a new template")
            .input(TFRegisterFileBundleRequest(UniqueId.alphanumeric(), UniqueId.alphanumeric()))
            .done()
            .build()
    }
}
