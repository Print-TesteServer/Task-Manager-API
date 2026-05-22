package com.delta.taskmanager.service

import com.delta.taskmanager.dto.auth.LoginRequest
import com.delta.taskmanager.dto.auth.RegisterRequest
import com.delta.taskmanager.entity.User
import com.delta.taskmanager.exception.EmailAlreadyExistsException
import com.delta.taskmanager.exception.InvalidCredentialsException
import com.delta.taskmanager.repository.UserRepository
import com.delta.taskmanager.security.JwtService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class AuthServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val jwtService: JwtService = mock()

    private val authService = AuthService(userRepository, passwordEncoder, jwtService)

    private fun mockUser(email: String = "test@email.com") = User(
        id = 1L,
        name = "Test User",
        email = email,
        password = "hashed-password"
    )

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    fun `register - success returns auth response with token`() {
        val request = RegisterRequest("Test User", "test@email.com", "password123")
        val user = mockUser()

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(passwordEncoder.encode(request.password)).thenReturn("hashed-password")
        whenever(userRepository.save(any<User>())).thenReturn(user)
        whenever(jwtService.generateToken(user.email)).thenReturn("jwt-token")

        val result = authService.register(request)

        assertEquals("jwt-token", result.token)
        assertEquals("Bearer", result.tokenType)
        assertEquals(user.email, result.email)
        assertEquals(user.name, result.name)
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `register - throws EmailAlreadyExistsException when email is taken`() {
        val request = RegisterRequest("Test User", "taken@email.com", "password123")
        whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

        assertThrows<EmailAlreadyExistsException> { authService.register(request) }
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `register - password is encoded before save`() {
        val request = RegisterRequest("User", "new@email.com", "plaintext")
        val user = mockUser("new@email.com")

        whenever(userRepository.existsByEmail(any<String>())).thenReturn(false)
        whenever(passwordEncoder.encode("plaintext")).thenReturn("encoded")
        whenever(userRepository.save(any<User>())).thenReturn(user)
        whenever(jwtService.generateToken(any<String>())).thenReturn("token")

        authService.register(request)

        verify(passwordEncoder).encode("plaintext")
        verify(userRepository).save(argThat { password == "encoded" })
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login - success returns auth response with token`() {
        val request = LoginRequest("test@email.com", "password123")
        val user = mockUser()

        whenever(userRepository.findByEmail(request.email)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches(request.password, user.password)).thenReturn(true)
        whenever(jwtService.generateToken(user.email)).thenReturn("jwt-token")

        val result = authService.login(request)

        assertEquals("jwt-token", result.token)
        assertEquals(user.email, result.email)
    }

    @Test
    fun `login - throws InvalidCredentialsException when user not found`() {
        val request = LoginRequest("unknown@email.com", "password123")
        whenever(userRepository.findByEmail(request.email)).thenReturn(Optional.empty())

        assertThrows<InvalidCredentialsException> { authService.login(request) }
    }

    @Test
    fun `login - throws InvalidCredentialsException when password is wrong`() {
        val request = LoginRequest("test@email.com", "wrong-password")
        val user = mockUser()

        whenever(userRepository.findByEmail(request.email)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches(request.password, user.password)).thenReturn(false)

        assertThrows<InvalidCredentialsException> { authService.login(request) }
        verify(jwtService, never()).generateToken(any<String>())
    }
}
