package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.BundleItemList
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.registry.Registry
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.UniqueId
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.rules.TestName
import java.io.File


class IntegrationTestTFInitModuleTask {
    @Rule
    var name = TestName()

    @Test
    fun `should do something`(testInfo: TestInfo) {
        val location = TestLocations(baseDir = "..")
        val sks = SimpleKVStore()
        val es = InMemoryEventStore()
        println("`${testInfo.testMethod.get().name}` results at ${location.homeDirectory()}")
        val reg = Registry().store(location).store(sks).store(es)

        val task = TFCreateModuleTask(reg)
        val ctx = SimpleExecutionContext()

        val mainDotTf = File("src/test/resources/main.tf").readText()

        val item = TextBundleItem("main.tf", mainDotTf)
        val list = BundleItemList(listOf(item))
        val bundleId = UniqueId.randomUUID()
        val x = FileBundle(bundleId, "bundle1", list)
        val req = TerraformModule(UniqueId.alphanumeric(), "module1")
        val result = task.exec(ctx,req)

        println(result)
        //task.exec(ctx,req)
    }
}