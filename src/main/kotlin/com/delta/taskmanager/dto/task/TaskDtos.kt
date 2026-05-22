package com.delta.taskmanager.dto.task

import com.delta.taskmanager.entity.Task
import com.delta.taskmanager.entity.TaskStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CreateTaskRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,

    val description: String? = null,

    val status: TaskStatus = TaskStatus.PENDING
)

data class UpdateTaskRequest(
    @field:NotBlank(message = "Title cannot be blank")
    val title: String? = null,

    val description: String? = null,

    val status: TaskStatus? = null
)

data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val createdAt: LocalDateTime,
    val userId: Long
) {
    companion object {
        fun from(task: Task) = TaskResponse(
            id = task.id,
            title = task.title,
            description = task.description,
            status = task.status,
            createdAt = task.createdAt,
            userId = task.user.id
        )
    }
}
