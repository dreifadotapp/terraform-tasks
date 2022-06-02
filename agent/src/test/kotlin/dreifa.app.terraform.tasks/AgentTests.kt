package dreifa.app.terraform.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.FileEventStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.client.SimpleClientContext
import dreifa.app.tasks.httpClient.HttpTaskClient
import dreifa.app.tasks.httpClient.TheClientApp
import dreifa.app.tasks.inbuilt.fileBundle.FBStoreTask
import dreifa.app.tasks.logging.InMemoryLoggingRepo
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentTests {
    private val config = Config()
    private val location = TestLocations(baseDir = "..")
    private val registry = Registry()
        .store(location)
        .store(InMemoryLoggingRepo())// need a common InMemoryLoggingRepo
        .store(FileEventStore(".testing"))
    private val app = AgentApp(registry, config)
    private val client = TheClientApp(registry)

    @BeforeAll
    fun `start`() {
        app.start()
        client.start()
    }

    @AfterAll
    fun `stop`() {
        app.stop()
        client.stop()
    }

    @Test
    fun `should call echo task`() {
        val client = HttpTaskClient(registry, "http://localhost:${config.port()}")
        val ctx = SimpleClientContext()

        val result = client.execBlocking(
            ctx, TFEchoTask::class.qualifiedName!!, "Hello", String::class
        )
        assertThat(result, equalTo("hello"))
    }

    @Test
    fun `should run simple terraform template`() {
        val client = HttpTaskClient(registry, "http://localhost:${config.port()}")
        val ctx = SimpleClientContext()

        // 1. register a new module
        val moduleId = UniqueId.randomUUID()
        val registerModuleRequest = TFRegisterModuleParams(moduleId = moduleId, moduleName = "test")
        client.execBlocking(
            ctx, TFRegisterModuleTask::class.qualifiedName!!, registerModuleRequest, Unit::class
        )

        // 2. upload bundle with terraform template
        val bundleId = UniqueId.randomUUID()
        val bundle = Fixtures.templateBundle(bundleId)
        val asText = TextAdapter().fromBundle(bundle)
        client.execBlocking(
            ctx, FBStoreTask::class.qualifiedName!!, asText, Unit::class
        )

        // 3. Register the file bundle
        val uploadRequest = TFRegisterFileBundleRequest(moduleId, bundleId)
        client.execBlocking(
            ctx, TFRegisterFileBundleTask::class.qualifiedName!!, uploadRequest, Unit::class
        )

        // 4. Initialise Terraform
        val initRequest = TFInitModuleRequest(moduleId)
        client.execBlocking(
            ctx, TFInitModuleTask::class.qualifiedName!!, initRequest, Unit::class
        )

        // 5. Apply Terraform
        val applyRequest = TFApplyModuleRequest(
            moduleId,
            mapOf("content" to "Hello World!")
        )
        client.execBlocking(
            ctx, TFApplyModuleTask::class.qualifiedName!!, applyRequest, Unit::class
        )

        // 6. Do we have the expected output
        val foobar = File("${location.serviceHomeDirectory("terraform", null)}/foo.bar")
        assert(foobar.exists()) { "expected to find `foo.bar` file" }
        assertThat(foobar.readText(), equalTo("Hello World!"))
    }
}
