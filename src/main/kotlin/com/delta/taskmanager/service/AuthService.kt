package com.delta.taskmanager.service

import com.delta.taskmanager.dto.auth.AuthResponse
import com.delta.taskmanager.dto.auth.LoginRequest
import com.delta.taskmanager.dto.auth.RegisterRequest
import com.delta.taskmanager.entity.User
import com.delta.taskmanager.exception.EmailAlreadyExistsException
import com.delta.taskmanager.exception.InvalidCredentialsException
import com.delta.taskmanager.repository.UserRepository
import com.delta.taskmanager.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password)
        )
        val saved = userRepository.save(user)
        val token = jwtService.generateToken(saved.email)

        return AuthResponse(
            token = token,
            userId = saved.id,
            name = saved.name,
            email = saved.email
        )
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        val token = jwtService.generateToken(user.email)

        return AuthResponse(
            token = token,
            userId = user.id,
            name = user.name,
            email = user.email
        )
    }
}
