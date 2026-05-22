package com.delta.taskmanager.controller

import com.delta.taskmanager.dto.task.CreateTaskRequest
import com.delta.taskmanager.dto.task.TaskResponse
import com.delta.taskmanager.dto.task.UpdateTaskRequest
import com.delta.taskmanager.entity.TaskStatus
import com.delta.taskmanager.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Task management (requires authentication)")
class TaskController(private val taskService: TaskService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    fun create(
        @Valid @RequestBody request: CreateTaskRequest,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, user.username))

    @GetMapping
    @Operation(summary = "List all tasks for the authenticated user")
    fun listAll(
        @RequestParam(required = false) status: TaskStatus?,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<List<TaskResponse>> =
        ResponseEntity.ok(taskService.listAll(user.username, status))

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific task by ID")
    fun findById(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.findById(id, user.username))

    @PutMapping("/{id}")
    @Operation(summary = "Update title, description or status of a task")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTaskRequest,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.update(id, request, user.username))

    @PatchMapping("/{id}/done")
    @Operation(summary = "Mark a task as DONE")
    fun markDone(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.markDone(id, user.username))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDetails
    ): ResponseEntity<Void> {
        taskService.delete(id, user.username)
        return ResponseEntity.noContent().build()
    }
}
