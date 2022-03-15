package dreifa.app.terraform.tasks

import dreifa.app.tasks.SimpleTaskRegistrations
import dreifa.app.tasks.TaskRegistration

class TFTasks: SimpleTaskRegistrations(
    listOf(
        TaskRegistration(TFRegisterModuleTask::class),
        TaskRegistration(TFInitModuleTask::class),
        TaskRegistration(TFApplyModuleTask::class),
        TaskRegistration(TFUploadTemplatesTask::class),
    )
)
