package dreifa.app.terraform.tasks

import dreifa.app.opentelemetry.JaegerOpenTelemetryProvider
import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.DefaultAsyncResultChannelSinkFactory
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.httpServer.TaskController
import dreifa.app.tasks.logging.CapturedOutputStream
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.InMemoryLogging
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

class AgentApp(registry: Registry, config: Config) {
    private val server: Http4kServer

    init {

        //val vhost = "http://localhost:${config.port()}"
        //val expectedConfig = File("config.yaml") // under docker we simply expect this to mapped to the working dir
        //val testConfig = File("src/test/resources/config.yaml")

        // base services
        if (!registry.contains(OpenTelemetryProvider::class.java)) {
            val provider = JaegerOpenTelemetryProvider(false, "app.dreifa.terraform.agent", config.jaegerEndpoint())
            val tracer = provider.sdk().getTracer("terraform-tasks")
            registry.store(provider).store(tracer)
        }

        if (!registry.contains(EventStore::class.java)) {
            registry.store(InMemoryEventStore(registry))
        }


        val sks = SimpleKVStore()
        val logConsumerContext = InMemoryLogging()
        val captured = CapturedOutputStream(logConsumerContext)
        val locations = TestLocations()

        registry
            .store(sks)
            .store(logConsumerContext)
            .store(captured)
            .store(locations)
        //.store(provider)
        //.store(tracer)

        // wirein logging channel
        val logChannelFactory = DefaultLoggingChannelFactory(registry)
        registry.store(logChannelFactory)

        registry.store(DefaultAsyncResultChannelSinkFactory())

        // wire in TaskFactory
        val taskFactory = TaskFactory(registry)
        taskFactory.register(TFTasks())
        taskFactory.register(DemoTasks())
        registry.store(taskFactory)

        // wire in TaskClient
        registry.store(SimpleTaskClient(registry))

        server = TaskController(registry).asServer(Jetty(config.port()))
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

}


fun main() {
    val config = Config()
    val app = AgentApp(Registry(), config)
    app.start()
}