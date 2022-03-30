package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.tasks.inbuilt.fileBundle.FBStoreTaskImpl
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
    fun `should run full lifecycle`() {
        val (baseReg, _, _) = buildRegistry()
        val (ctx, _) = buildExecutionContext()
        val moduleId = UniqueId.alphanumeric()

        isolatedRun(baseReg) { reg, _ ->
            // 1. create a new module
            val createRequest = TFRegisterModuleParams(moduleId, "module1")
            TFRegisterModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, createRequest)
        }

        val bundleId = UniqueId.randomUUID()

        isolatedRun(baseReg) { reg, _ ->
            // 2a. create the file bundle with the template, and store it the KV store
            val bundle = Fixtures.templateBundle(bundleId)
            val asText = TextAdapter().fromBundle(bundle)
            FBStoreTaskImpl(reg).exec(ctx,asText)
        }

        isolatedRun(baseReg) { reg, _ ->
            // 2b. link the FileBundle to the module
            val uploadRequest = TFRegisterFileBundleRequest(moduleId, bundleId)
            TFRegisterFileBundleTaskImpl(reg).exec(ctx, uploadRequest)
        }

        isolatedRun(baseReg) { reg, _ ->
            // 3. run the 'terraform init' command
            val initRequest = TFInitModuleRequest(moduleId)
            TFInitModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, initRequest)
        }

        isolatedRun(baseReg) { reg, location ->
            // 3. run the 'terraform apply' command
            val applyRequest = TFApplyModuleRequest(
                moduleId,
                mapOf("content" to "Hello World!")
            )

            TFApplyModuleTaskImpl(reg).exec(ctx, applyRequest)

            val foobar = File("${location.serviceHomeDirectory(ctx, "terraform")}/foo.bar")
            assert(foobar.exists()) { "expected to find `foo.bar` file" }
            assertThat(foobar.readText(), equalTo("Hello World!"))
        }

        isolatedRun(baseReg) { reg, location ->
            // 3a. reapply with default variables
            val applyRequest = TFApplyModuleRequest(
                moduleId
            )

            TFApplyModuleTaskImpl(reg).exec(ctx, applyRequest)

            val foobar = File("${location.serviceHomeDirectory(ctx, "terraform")}/foo.bar")
            assert(foobar.exists()) { "expected to find `foo.bar` file" }
            assertThat(foobar.readText(), equalTo("foo!"))
        }
    }
}