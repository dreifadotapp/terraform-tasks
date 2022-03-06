package dreifa.app.terraform.tasks

import dreifa.app.registry.Registry
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
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
        println("`${testInfo.testMethod.get().name}` results at ${location.homeDirectory()}")
        val reg = Registry().store(location)

        val task = TFInitModuleTask(reg)

        val ctx = SimpleExecutionContext()

        val mainDotTf = File("src/test/resources/main.tf").readText()
        //val x = FileContent("main.tf", mainDotTf)
        //val req = TFCreateModuleRequest(UniqueId.alphanumeric(), FileContentList(listOf(x)))

        //task.exec(ctx,req)
    }
}