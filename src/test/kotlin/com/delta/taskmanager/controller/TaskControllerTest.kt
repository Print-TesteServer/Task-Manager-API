package com.delta.taskmanager.controller

import com.delta.taskmanager.dto.task.CreateTaskRequest
import com.delta.taskmanager.dto.task.TaskResponse
import com.delta.taskmanager.dto.task.UpdateTaskRequest
import com.delta.taskmanager.entity.TaskStatus
import com.delta.taskmanager.config.WebMvcTestSecurityConfig
import com.delta.taskmanager.exception.ForbiddenException
import com.delta.taskmanager.exception.GlobalExceptionHandler
import com.delta.taskmanager.exception.ResourceNotFoundException
import com.delta.taskmanager.service.TaskService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@WebMvcTest(
    controllers = [TaskController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@Import(GlobalExceptionHandler::class, WebMvcTestSecurityConfig::class)
@ActiveProfiles("test")
class TaskControllerTest {

    companion object {
        private const val USER_EMAIL = "user@email.com"
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var taskService: TaskService

    private fun taskResponse(id: Long = 1L) = TaskResponse(
        id = id,
        title = "My Task",
        description = "Description",
        status = TaskStatus.PENDING,
        createdAt = LocalDateTime.now(),
        userId = 1L
    )

    // ── POST /api/tasks ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `POST tasks - 201 with created task`() {
        val request = CreateTaskRequest("My Task", "Description")
        whenever(taskService.create(any(), eq(USER_EMAIL))).thenReturn(taskResponse())

        mockMvc.post("/api/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("My Task") }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `POST tasks - 400 when title is blank`() {
        val request = mapOf("title" to "")

        mockMvc.post("/api/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ── GET /api/tasks ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `GET tasks - 200 with list of tasks`() {
        val tasks = listOf(taskResponse(1L), taskResponse(2L))
        whenever(taskService.listAll(eq(USER_EMAIL), eq(null))).thenReturn(tasks)

        mockMvc.get("/api/tasks").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun `GET tasks - 401 when not authenticated`() {
        mockMvc.get("/api/tasks").andExpect {
            status { isUnauthorized() }
        }
    }

    // ── GET /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `GET tasks id - 200 when task exists and belongs to user`() {
        whenever(taskService.findById(1L, USER_EMAIL)).thenReturn(taskResponse())

        mockMvc.get("/api/tasks/1").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
        }
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `GET tasks id - 404 when task not found`() {
        whenever(taskService.findById(99L, USER_EMAIL))
            .thenThrow(ResourceNotFoundException("Task not found with id: 99"))

        mockMvc.get("/api/tasks/99").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `GET tasks id - 403 when task belongs to another user`() {
        whenever(taskService.findById(1L, USER_EMAIL))
            .thenThrow(ForbiddenException("You do not have access to this task"))

        mockMvc.get("/api/tasks/1").andExpect {
            status { isForbidden() }
        }
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `PUT tasks id - 200 with updated task`() {
        val request = UpdateTaskRequest(title = "Updated", status = TaskStatus.IN_PROGRESS)
        val updated = taskResponse().copy(title = "Updated", status = TaskStatus.IN_PROGRESS)
        whenever(taskService.update(eq(1L), any(), eq(USER_EMAIL))).thenReturn(updated)

        mockMvc.put("/api/tasks/1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Updated") }
            jsonPath("$.status") { value("IN_PROGRESS") }
        }
    }

    // ── PATCH /api/tasks/{id}/done ────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `PATCH tasks id done - 200 with task marked DONE`() {
        val done = taskResponse().copy(status = TaskStatus.DONE)
        whenever(taskService.markDone(1L, USER_EMAIL)).thenReturn(done)

        mockMvc.patch("/api/tasks/1/done").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DONE") }
        }
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `DELETE tasks id - 204 on success`() {
        mockMvc.delete("/api/tasks/1").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    fun `DELETE tasks id - 403 when another user tries to delete`() {
        whenever(taskService.delete(1L, USER_EMAIL))
            .thenThrow(ForbiddenException("You do not have access to this task"))

        mockMvc.delete("/api/tasks/1").andExpect {
            status { isForbidden() }
        }
    }
}
