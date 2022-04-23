package dreifa.app.terraform.tasks

import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage

/**
 * A Basic Echo Task for debugging and testing only.
 * The input is echoed back, with all in
 */
class TFEchoTask : BlockingTask<String, String>, TaskDoc<String, String> {
    override fun exec(ctx: ExecutionContext, input: String): String {
        ctx.log(LogMessage.info("TFPingTask called with $input"))
        return input.lowercase()
    }

    override fun description() =
        """ A Basic Echo Task for debugging and testing only.
            The input is echoed back, with all input in lowercase
       """.trimIndent()

    override fun examples(): List<TaskExample<String, String>> {
        return TaskExamplesBuilder()
            .example("Hello World")
            .input("Hello, World")
            .output("hello, world")
            .done()
            .build()
    }
}