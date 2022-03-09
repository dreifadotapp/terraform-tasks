package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.registry.Registry
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.UniqueId
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.rules.TestName
import java.io.File

class LifeCycleTest : BaseTestCase() {
    @Rule
    var name = TestName()

    @Test
    fun `should run full lifecycle`(testInfo: TestInfo) {
        val sks = SimpleKVStore()
        val es = InMemoryEventStore()
        val reg = Registry().store(sks).store(es)

        val ctx = SimpleExecutionContext().withInstanceQualifier("module1")
        val moduleId = UniqueId.alphanumeric()

        isolatedRun(reg) { reg, _ ->
            // 1. create a new module
            val createRequest = TFRegisterModuleParams(moduleId, "module1")
            TFRegisterModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, createRequest)
        }

        val bundleId = UniqueId.randomUUID()
        isolatedRun(reg) { reg, _ ->
            // 2. create the file bundle with the template, and store it the KV store
            val bundle = Fixtures.templateBundle(bundleId)
            val uploadRequest = TFUploadTemplatesRequest(moduleId, bundle)
            TFUploadTemplatesTaskImpl(reg).exec(ctx, uploadRequest)
        }

        isolatedRun(reg) { reg, _ ->
            // 3. run the 'terraform init' command
            val initRequest = TFInitModuleRequest(moduleId, bundleId)
            TFInitModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, initRequest)
        }

        isolatedRun(reg) { reg, location ->
            // 3. run the 'terraform apply' command
            val applyRequest = TFApplyModuleRequest(
                moduleId,
                bundleId,
                mapOf("content" to "Hello World!")
            )

            TFApplyModuleTaskImpl(reg).exec(ctx, applyRequest)

            val foobar = File("${location.serviceHomeDirectory(ctx, "terraform")}/foo.bar")
            assert(foobar.exists()) { "expected to find `foo.bar` file" }
            assertThat(foobar.readText(), equalTo("Hello World!"))
        }

        isolatedRun(reg) { reg, location ->
            // 3a. reapply with default variables
            val applyRequest = TFApplyModuleRequest(
                moduleId,
                bundleId
            )

            TFApplyModuleTaskImpl(reg).exec(ctx, applyRequest)

            val foobar = File("${location.serviceHomeDirectory(ctx, "terraform")}/foo.bar")
            assert(foobar.exists()) { "expected to find `foo.bar` file" }
            assertThat(foobar.readText(), equalTo("foo!"))
        }
    }
}