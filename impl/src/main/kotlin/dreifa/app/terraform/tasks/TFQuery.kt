package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.AggregateIdQuery
import dreifa.app.ses.AllOfQuery
import dreifa.app.ses.EventStore
import dreifa.app.ses.EventTypeQuery
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

class TFQuery(registry: Registry) {
    private val ses = registry.get(EventStore::class.java)

    fun moduleRegistered(moduleId: UniqueId): Boolean {
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = ModuleRegisteredEventFactory.eventType()),
                AggregateIdQuery(aggregateId = moduleId.toString())
            )
        )
        return ses.read(query).isNotEmpty()
    }

    fun bundleId(moduleId: UniqueId): UniqueId {
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = TemplateUploadedEventFactory.eventType()),
                AggregateIdQuery(aggregateId = moduleId.toString())
            )
        )
        return if (ses.read(query).isNotEmpty()) {
            val id = ses.read(query).single().payload
            id as UniqueId
        } else {
            throw RuntimeException("No bundle for module $moduleId")
        }
    }
}