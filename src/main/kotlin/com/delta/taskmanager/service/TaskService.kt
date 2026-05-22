package com.delta.taskmanager.service

import com.delta.taskmanager.dto.task.CreateTaskRequest
import com.delta.taskmanager.dto.task.TaskResponse
import com.delta.taskmanager.dto.task.UpdateTaskRequest
import com.delta.taskmanager.entity.Task
import com.delta.taskmanager.entity.TaskStatus
import com.delta.taskmanager.exception.ForbiddenException
import com.delta.taskmanager.exception.ResourceNotFoundException
import com.delta.taskmanager.repository.TaskRepository
import com.delta.taskmanager.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(request: CreateTaskRequest, email: String): TaskResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("User not found: $email") }

        val task = Task(
            title = request.title,
            description = request.description,
            status = request.status,
            user = user
        )
        return TaskResponse.from(taskRepository.save(task))
    }

    @Transactional(readOnly = true)
    fun listAll(email: String, status: TaskStatus?): List<TaskResponse> {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("User not found: $email") }

        return if (status != null) {
            taskRepository.findAllByUserIdAndStatus(user.id, status)
        } else {
            taskRepository.findAllByUserId(user.id)
        }.map(TaskResponse::from)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long, email: String): TaskResponse {
        val task = resolveTask(id, email)
        return TaskResponse.from(task)
    }

    @Transactional
    fun update(id: Long, request: UpdateTaskRequest, email: String): TaskResponse {
        val task = resolveTask(id, email)

        request.title?.let { task.title = it }
        request.description?.let { task.description = it }
        request.status?.let { task.status = it }

        return TaskResponse.from(taskRepository.save(task))
    }

    @Transactional
    fun markDone(id: Long, email: String): TaskResponse {
        val task = resolveTask(id, email)
        task.status = TaskStatus.DONE
        return TaskResponse.from(taskRepository.save(task))
    }

    @Transactional
    fun delete(id: Long, email: String) {
        val task = resolveTask(id, email)
        taskRepository.delete(task)
    }

    private fun resolveTask(id: Long, email: String): Task {
        val task = taskRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Task not found with id: $id") }

        if (task.user.email != email) {
            throw ForbiddenException("You do not have access to this task")
        }
        return task
    }
}
