package com.delta.taskmanager.controller

import com.delta.taskmanager.dto.auth.AuthResponse
import com.delta.taskmanager.dto.auth.LoginRequest
import com.delta.taskmanager.dto.auth.RegisterRequest
import com.delta.taskmanager.exception.EmailAlreadyExistsException
import com.delta.taskmanager.exception.GlobalExceptionHandler
import com.delta.taskmanager.exception.InvalidCredentialsException
import com.delta.taskmanager.service.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var authService: AuthService

    private val authResponse = AuthResponse(
        token = "jwt-token",
        userId = 1L,
        name = "Test User",
        email = "test@email.com"
    )

    @Test
    @WithAnonymousUser
    fun `POST register - 201 with token on success`() {
        val request = RegisterRequest("Test User", "test@email.com", "password123")
        whenever(authService.register(any())).thenReturn(authResponse)

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.token") { value("jwt-token") }
            jsonPath("$.email") { value("test@email.com") }
            jsonPath("$.tokenType") { value("Bearer") }
        }
    }

    @Test
    @WithAnonymousUser
    fun `POST register - 400 when fields are blank`() {
        val request = mapOf("name" to "", "email" to "bad-email", "password" to "123")

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Validation Failed") }
        }
    }

    @Test
    @WithAnonymousUser
    fun `POST register - 409 when email already exists`() {
        val request = RegisterRequest("User", "taken@email.com", "password123")
        whenever(authService.register(any())).thenThrow(EmailAlreadyExistsException("taken@email.com"))

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithAnonymousUser
    fun `POST login - 200 with token on success`() {
        val request = LoginRequest("test@email.com", "password123")
        whenever(authService.login(any())).thenReturn(authResponse)

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { value("jwt-token") }
        }
    }

    @Test
    @WithAnonymousUser
    fun `POST login - 401 on invalid credentials`() {
        val request = LoginRequest("test@email.com", "wrong")
        whenever(authService.login(any())).thenThrow(InvalidCredentialsException())

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
