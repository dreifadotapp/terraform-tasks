package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isEmpty
import dreifa.app.registry.Registry
import dreifa.app.ses.EverythingQuery
import dreifa.app.ses.InMemoryEventStore

import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.InMemoryLogging
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TFCreateModuleTaskTest {

    @Test
    fun `should create module`() {
        val reg = Registry()
        val ses = InMemoryEventStore()
        reg.store(ses)

        val t = TFCreateModuleTask(reg)
        val logging = InMemoryLogging()
        val ctx = SimpleExecutionContext().withInMemoryLogging(logging)

        assertThat(ses.read(EverythingQuery), isEmpty)
        val module = TerraformModule(id = UniqueId.randomUUID(), name = "demo")
        t.exec(ctx, module)

        println(logging.messages().first())

        assertThat(ses.read(EverythingQuery), !isEmpty)
    }
}