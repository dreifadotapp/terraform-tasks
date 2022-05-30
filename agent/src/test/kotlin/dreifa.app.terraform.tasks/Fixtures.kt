package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.types.UniqueId
import java.io.File

object Fixtures {

    fun templateBundle(bundleId: UniqueId = UniqueId.randomUUID()): FileBundle {
        return FileBundleBuilder()
            .withId(bundleId)
            .withName("Terraform Bundle")
            .addItem(TextBundleItem("main.tf", File("src/test/resources/localfile/main.tf").readText()))
            .build()
    }

}