package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSKeyValue
import dreifa.app.sks.SKSValueType
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId

data class TFUploadModuleRequest(val moduleId: UniqueId, val bundle: FileBundle)

class TFUploadModuleTask(registry: Registry) : BaseBlockingTask<TFUploadModuleRequest, Unit>() {
    private val sks = registry.get(SKS::class.java)
    private val bundleAdapter = TextAdapter()
    override fun exec(ctx: ExecutionContext, input: TFUploadModuleRequest) {

        val text = bundleAdapter.fromBundle(input.bundle)
        val kv = SKSKeyValue(
            Key.fromUniqueId(input.bundle.id),
            text,
            SKSValueType.Text
        )
        sks.put(kv)
    }
}

fun main(args: Array<String>) {
    val reg = Registry()
    val location = TestLocations(baseDir = "..")
    val sks = SimpleKVStore()
    reg.store(sks).store(location)

    val template = """
terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 2.13.0"
    }
  }
}

provider "docker" {}

resource "docker_image" "nginx" {
  name         = "nginx:latest"
  keep_locally = false
}

resource "docker_container" "nginx" {
  image = docker_image.nginx.latest
  name  = "tutorial"
  ports {
    internal = 80
    external = 8000
  }
} 
    """.trimIndent()

    val bundleId = UniqueId.randomUUID()
    val moduleId = UniqueId.randomUUID()
    val bundle = FileBundleBuilder()
        .withId(bundleId)
        .withName("Simple-Terraform-Demo")
        .addItem(TextBundleItem("main.tf", template))
        .build()

    val task = TFUploadModuleTask(reg)

    val ctx = SimpleExecutionContext()
    task.exec(ctx, TFUploadModuleRequest(moduleId, bundle))



    //println(sks.get(Key.fromUniqueId(moduleId)))


}