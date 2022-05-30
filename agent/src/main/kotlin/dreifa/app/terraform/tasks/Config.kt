package dreifa.app.terraform.tasks

class Config {
    private val prefix = "dreifa.app.terraform.tasks"
    private val env: Map<String, String> = System.getenv() as Map<String, String>

    fun jaegerEndpoint(): String {
        val key = "$prefix.jaegerEndpoint".replace('.','_')
        val result =  env.getOrDefault(key, "http://localhost:14250")
        println ("jaegerEndpoint is `$result`")
        return result
    }
}