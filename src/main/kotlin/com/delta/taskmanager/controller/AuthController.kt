package com.delta.taskmanager.controller

import com.delta.taskmanager.dto.auth.AuthResponse
import com.delta.taskmanager.dto.auth.LoginRequest
import com.delta.taskmanager.dto.auth.RegisterRequest
import com.delta.taskmanager.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT token")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))
}
