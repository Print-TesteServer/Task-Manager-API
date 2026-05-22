package com.delta.taskmanager.integration

import com.delta.taskmanager.dto.auth.AuthResponse
import com.delta.taskmanager.dto.auth.RegisterRequest
import com.delta.taskmanager.dto.task.CreateTaskRequest
import com.delta.taskmanager.entity.TaskStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskManagerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `full flow - register login create task list and mark done`() {
        val suffix = UUID.randomUUID().toString().take(8)
        val email = "user-$suffix@test.com"
        val password = "senha123"

        val token = registerAndGetToken("User One", email, password)

        val createResult = mockMvc.post("/api/tasks") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CreateTaskRequest(
                    title = "Integration task",
                    description = "E2E test",
                    status = TaskStatus.PENDING
                )
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Integration task") }
            jsonPath("$.status") { value("PENDING") }
        }.andReturn()

        val taskId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        mockMvc.get("/api/tasks") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }

        mockMvc.patch("/api/tasks/$taskId/done") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DONE") }
        }
    }

    @Test
    fun `another user cannot access task - returns 403`() {
        val suffix = UUID.randomUUID().toString().take(8)
        val tokenA = registerAndGetToken("Owner", "owner-$suffix@test.com", "senha123")
        val tokenB = registerAndGetToken("Other", "other-$suffix@test.com", "senha456")

        val createResult = mockMvc.post("/api/tasks") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTaskRequest(title = "Private task"))
        }.andExpect { status { isCreated() } }.andReturn()

        val taskId = objectMapper.readTree(createResult.response.contentAsString)["id"].asLong()

        mockMvc.get("/api/tasks/$taskId") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `tasks endpoint without token - returns 401`() {
        mockMvc.get("/api/tasks").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `GET on POST-only register endpoint - returns 405`() {
        mockMvc.get("/api/auth/register").andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.status") { value(405) }
            jsonPath("$.error") { value("Method Not Allowed") }
        }
    }

    @Test
    fun `register duplicate email - returns 409`() {
        val email = "dup-${UUID.randomUUID().toString().take(8)}@test.com"
        registerAndGetToken("First", email, "senha123")

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(name = "Second", email = email, password = "senha456")
            )
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { value("Conflict") }
        }
    }

    private fun registerAndGetToken(name: String, email: String, password: String): String {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(name = name, email = email, password = password)
            )
        }.andExpect { status { isCreated() } }.andReturn()

        return objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java).token
    }
}
