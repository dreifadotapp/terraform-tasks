package dreifa.app.terraform.tasks

import dreifa.app.opentelemetry.JaegerOpenTelemetryProvider
import dreifa.app.registry.Registry
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.DefaultAsyncResultChannelSinkFactory
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.httpServer.TaskController
import dreifa.app.tasks.inbuilt.InBuiltTasks
import dreifa.app.tasks.logging.CapturedOutputStream
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.InMemoryLogging
import org.http4k.server.Jetty
import org.http4k.server.asServer


fun main() {
    val config = Config()
    val port = 8088
    val vhost = "http://localhost:$port"
    //val expectedConfig = File("config.yaml") // under docker we simply expect this to mapped to the working dir
    //val testConfig = File("src/test/resources/config.yaml")

    // base services
    val registry = Registry()
    val provider = JaegerOpenTelemetryProvider(false, "app.dreifa.terraform.agent", config.jaegerEndpoint())
    val tracer = provider.sdk().getTracer("local-tasks")
    registry.store(provider).store(tracer)

    val es = InMemoryEventStore(registry)
    val sks = SimpleKVStore()
    val logConsumerContext = InMemoryLogging()
    val captured = CapturedOutputStream(logConsumerContext)
    val locations = TestLocations()

    registry.store(es)
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
    taskFactory.register(DemoTasks())
    taskFactory.register(EchoTasks())
    taskFactory.register(InBuiltTasks())
    registry.store(taskFactory)

    // wire in TaskClient
    registry.store(SimpleTaskClient(registry))

    val server = TaskController(registry).asServer(Jetty(port))
    println("Server started on $port")


}