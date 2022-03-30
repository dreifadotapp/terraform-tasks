package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.Locations
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.InMemoryLogging
import dreifa.app.tasks.logging.LoggingReaderContext

open class BaseTestCase {

    protected fun buildRegistry(): Triple<Registry, EventStore, SKS> {
        val ses = InMemoryEventStore()
        val sks = SimpleKVStore()
        val reg = Registry().store(ses).store(sks).store(TestLocations(baseDir = ".."))
        return Triple(reg, ses, sks)
    }

    protected fun buildExecutionContext(): Pair<ExecutionContext, LoggingReaderContext> {
        val logging = InMemoryLogging()
        val ctx = SimpleExecutionContext().withInMemoryLogging(logging).withInstanceQualifier("testing")
        return Pair(ctx, logging)
    }

    protected fun registryWithNewLocation(reg: Registry): Registry = reg.clone().store(TestLocations(baseDir = ".."))

    /**
     * Emulate the serverless model whereby we assume a fresh environment
     * for invocation of a Task
     */
    protected fun isolatedRun(
        reg: Registry,
        block: (reg: Registry, location: Locations) -> Unit
    ) {
        val location = TestLocations(baseDir = "..")
        val newRegistry = reg.clone().store(location)
        block.invoke(newRegistry, location)
    }
}