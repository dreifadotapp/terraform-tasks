package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.Event
import dreifa.app.ses.EventFactory
import dreifa.app.ses.EventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSKeyValue
import dreifa.app.sks.SKSValueType
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId

/**
 * The Task to upload the Terraform templates. Must have called TFRegisterModuleTask before
 * hand.
 */
data class TFUploadTemplatesRequest(val moduleId: UniqueId, val bundle: FileBundle)
interface TFUploadTemplatesTask : BlockingTask<TFUploadTemplatesRequest, Unit>, IdempotentTask

object TemplateUploadedEventFactory : EventFactory {
    fun create(params: TFUploadTemplatesRequest): Event {
        return Event(
            type = eventType(),
            aggregateId = params.moduleId.toString(),
            payload = params.bundle.id
        )
    }

    override fun eventType(): String = "dreifa.app.terraform.tasks.TemplateUploaded"
}

class TFUploadTemplatesTaskImpl(registry: Registry) : BaseBlockingTask<TFUploadTemplatesRequest, Unit>(),
    TFUploadTemplatesTask {
    private val sks = registry.get(SKS::class.java)
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: TFUploadTemplatesRequest) {
        // persist bundle in the key-value store
        val bundleAdapter = TextAdapter()
        val text = bundleAdapter.fromBundle(input.bundle)
        val kv = SKSKeyValue(
            Key.fromUniqueId(input.bundle.id),
            text,
            SKSValueType.Text
        )
        sks.put(kv)

        // record with event
        ses.store(TemplateUploadedEventFactory.create(input))
    }
}
