package dreifa.app.terraform.tasks

import dreifa.app.tasks.SimpleTaskRegistrations
import dreifa.app.tasks.TaskRegistration

class TFTasks : SimpleTaskRegistrations(
    listOf(
        TaskRegistration(TFEchoTask::class),
        TaskRegistration(TFRegisterModuleTaskImpl::class, TFRegisterModuleTask::class),
        TaskRegistration(TFInitModuleTaskImpl::class, TFInitModuleTask::class),
        TaskRegistration(TFApplyModuleTaskImpl::class, TFApplyModuleTask::class),
        TaskRegistration(TFUploadTemplatesTaskImpl::class, TFUploadTemplatesTask::class),
    )
)
