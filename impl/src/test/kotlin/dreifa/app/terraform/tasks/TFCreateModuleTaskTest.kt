package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.EverythingQuery
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext

import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.InMemoryLogging
import dreifa.app.tasks.logging.LoggingReaderContext
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TFCreateModuleTaskTest {

    @Test
    fun `should create module`() {
        val (reg, ses) = buildRegistry()
        val (ctx, logging) = buildExecutionContext()

        assertThat(ses.read(EverythingQuery), isEmpty)
        val module = TFCreateModuleParams(moduleId = UniqueId.randomUUID(), moduleName = "demo")
        TFCreateModuleTaskImpl(reg).exec(ctx, module)

        println(logging.messages().first())
        assertThat(ses.read(EverythingQuery).size, equalTo(1))
    }

    private fun buildRegistry(): Pair<Registry, EventStore> {
        val ses = InMemoryEventStore()
        val reg = Registry().store(ses).store(TestLocations(baseDir = ".."))
        return Pair(reg, ses)
    }

    private fun buildExecutionContext(): Pair<ExecutionContext, LoggingReaderContext> {
        val logging = InMemoryLogging()
        val ctx = SimpleExecutionContext().withInMemoryLogging(logging).withInstanceQualifier("testing")
        return Pair(ctx, logging)
    }
}