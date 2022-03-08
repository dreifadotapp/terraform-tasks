package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKSValueType
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.rules.TestName
import java.io.File

class LifeCycleTest {
    @Rule
    var name = TestName()

    @Test
    fun `should do something`(testInfo: TestInfo) {
        val sks = SimpleKVStore()
        val es = InMemoryEventStore()
        val reg = Registry().store(sks).store(es)

        val ctx = SimpleExecutionContext().withInstanceQualifier("module1")
        val moduleId = UniqueId.alphanumeric()

        // 1. create a new module
        val createRequest = TFCreateModuleParams(moduleId, "module1")
        TFCreateModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, createRequest)

        // 2. create the file bundle with the template, and store it the KV store
        val bundleId = UniqueId.randomUUID()
        val bundle = FileBundleBuilder()
            .withId(bundleId)
            .withName("Terraform Bundle")
            .addItem(TextBundleItem("main.tf", File("src/test/resources/localfile/main.tf").readText()))
            .build()
        sks.put(
            Key.fromUniqueId(bundleId),
            TextAdapter().fromBundle(bundle),
            SKSValueType.Text
        )

        // 3. run the 'terraform init' command
        val initRequest = TFInitModuleRequest(moduleId, bundleId)
        val initResult = TFInitModuleTaskImpl(registryWithNewLocation(reg)).exec(ctx, initRequest)
        println(initResult)

        // 4. run the 'terraform apply' command
        val applyRequest = TFApplyModuleRequest(moduleId, bundleId)
        val applyResult = TFApplyModuleTask(registryWithNewLocation(reg)).exec(ctx, applyRequest)
        println(applyResult)
    }

    private fun registryWithNewLocation(reg: Registry): Registry = reg.clone().store(TestLocations(baseDir = ".."))
}