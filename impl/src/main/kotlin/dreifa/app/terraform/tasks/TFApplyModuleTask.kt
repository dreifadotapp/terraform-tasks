package dreifa.app.terraform.tasks

import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.fileBundle.builders.ScanDirectoryBuilder
import dreifa.app.registry.Registry
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
import java.util.*
import dreifa.app.types.UniqueId
import java.io.File

data class TFApplyModuleRequest(val moduleId: UniqueId, val bundleId: UniqueId)

class TFApplyModuleTask(registry: Registry) : BaseTerraformTask<TFApplyModuleRequest, String>(registry) {

    override fun exec(ctx: ExecutionContext, input: TFApplyModuleRequest): String {

        val location = location(ctx)
        val bundle = recoverBundle(location, input.bundleId)

        val pb = ProcessBuilder(terraform(), "apply", "-auto-approve")
            .directory(File(File(location).absolutePath))
        val processId = UUID.randomUUID()

        val pm = ctx.processManager()
        pm.registerProcess(pb, processId, "terraform-apply")
        waitForProcess(ctx, processId)
        val output = pm.lookupOutput(processId)

        return output!!.stdout.toString()
        //return ""
    }
}


fun main(args: Array<String>) {
    val reg = Registry()
    val sks = SimpleKVStore()
    val location1 = TestLocations(baseDir = ".")
    reg.store(sks).store(location1)

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

    val ctx = SimpleExecutionContext()
    TFUploadModuleTask(reg).exec(ctx, TFUploadModuleRequest(moduleId, bundle))
    val result1 = TFInitModuleTaskImpl(reg).exec(ctx, TFInitModuleRequest(moduleId, bundleId))
    println(result1)

    val location2 = TestLocations(baseDir = ".")
    reg.store(location2)
    val result2 = TFApplyModuleTask(reg).exec(ctx, TFApplyModuleRequest(moduleId, bundleId))
    println(result2)
}