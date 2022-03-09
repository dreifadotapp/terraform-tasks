package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import dreifa.app.ses.EverythingQuery
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TFRegisterModuleTaskTest : BaseTestCase() {

    @Test
    fun `should create module`() {
        val (reg, ses) = buildRegistry()
        val (ctx, logging) = buildExecutionContext()

        assertThat(ses.read(EverythingQuery), isEmpty)
        val module = TFRegisterModuleParams(moduleId = UniqueId.randomUUID(), moduleName = "demo")
        TFRegisterModuleTaskImpl(reg).exec(ctx, module)

        println(logging.messages().first())
        assertThat(ses.read(EverythingQuery).size, equalTo(1))
    }

}