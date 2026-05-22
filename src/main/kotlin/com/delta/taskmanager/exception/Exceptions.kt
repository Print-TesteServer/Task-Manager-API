package com.delta.taskmanager.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)

class ForbiddenException(message: String = "Access denied") : RuntimeException(message)

class EmailAlreadyExistsException(email: String) :
    RuntimeException("Email already in use: $email")

class InvalidCredentialsException(message: String = "Invalid email or password") :
    RuntimeException(message)
