package com.delta.taskmanager.service

import com.delta.taskmanager.dto.task.CreateTaskRequest
import com.delta.taskmanager.dto.task.UpdateTaskRequest
import com.delta.taskmanager.entity.Task
import com.delta.taskmanager.entity.TaskStatus
import com.delta.taskmanager.entity.User
import com.delta.taskmanager.exception.ForbiddenException
import com.delta.taskmanager.exception.ResourceNotFoundException
import com.delta.taskmanager.repository.TaskRepository
import com.delta.taskmanager.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional

class TaskServiceTest {

    private val taskRepository: TaskRepository = mock()
    private val userRepository: UserRepository = mock()

    private val taskService = TaskService(taskRepository, userRepository)

    private val ownerEmail = "owner@email.com"
    private val otherEmail = "other@email.com"

    private fun mockUser(email: String = ownerEmail) = User(
        id = 1L, name = "Owner", email = email, password = "hashed"
    )

    private fun mockTask(user: User = mockUser(), id: Long = 1L) = Task(
        id = id,
        title = "My Task",
        description = "Description",
        status = TaskStatus.PENDING,
        user = user
    )

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    fun `create - saves task and returns response`() {
        val request = CreateTaskRequest("New Task", "desc", TaskStatus.PENDING)
        val user = mockUser()
        val savedTask = mockTask(user)

        whenever(userRepository.findByEmail(ownerEmail)).thenReturn(Optional.of(user))
        whenever(taskRepository.save(any())).thenReturn(savedTask)

        val result = taskService.create(request, ownerEmail)

        assertEquals("My Task", result.title)
        assertEquals(TaskStatus.PENDING, result.status)
        verify(taskRepository).save(any())
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    fun `listAll - returns only tasks belonging to the authenticated user`() {
        val user = mockUser()
        val tasks = listOf(mockTask(user, 1L), mockTask(user, 2L))

        whenever(userRepository.findByEmail(ownerEmail)).thenReturn(Optional.of(user))
        whenever(taskRepository.findAllByUserId(user.id)).thenReturn(tasks)

        val result = taskService.listAll(ownerEmail, null)

        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == user.id })
    }

    @Test
    fun `listAll - filters by status when provided`() {
        val user = mockUser()
        val doneTasks = listOf(mockTask(user).apply { status = TaskStatus.DONE })

        whenever(userRepository.findByEmail(ownerEmail)).thenReturn(Optional.of(user))
        whenever(taskRepository.findAllByUserIdAndStatus(user.id, TaskStatus.DONE)).thenReturn(doneTasks)

        val result = taskService.listAll(ownerEmail, TaskStatus.DONE)

        assertEquals(1, result.size)
        assertEquals(TaskStatus.DONE, result[0].status)
        verify(taskRepository).findAllByUserIdAndStatus(user.id, TaskStatus.DONE)
        verify(taskRepository, never()).findAllByUserId(any())
    }

    // ── Find by ID ────────────────────────────────────────────────────────────

    @Test
    fun `findById - returns task when owner requests it`() {
        val user = mockUser()
        val task = mockTask(user)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))

        val result = taskService.findById(1L, ownerEmail)

        assertEquals(task.id, result.id)
        assertEquals(task.title, result.title)
    }

    @Test
    fun `findById - throws ResourceNotFoundException when task does not exist`() {
        whenever(taskRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { taskService.findById(99L, ownerEmail) }
    }

    @Test
    fun `findById - throws ForbiddenException when another user tries to access`() {
        val owner = mockUser(ownerEmail)
        val task = mockTask(owner)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))

        assertThrows<ForbiddenException> { taskService.findById(1L, otherEmail) }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    fun `update - modifies only provided fields`() {
        val user = mockUser()
        val task = mockTask(user)
        val request = UpdateTaskRequest(title = "Updated Title", status = TaskStatus.IN_PROGRESS)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))
        whenever(taskRepository.save(any())).thenReturn(task)

        taskService.update(1L, request, ownerEmail)

        assertEquals("Updated Title", task.title)
        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        assertEquals("Description", task.description) // unchanged
    }

    @Test
    fun `update - throws ForbiddenException for non-owner`() {
        val owner = mockUser(ownerEmail)
        val task = mockTask(owner)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))

        assertThrows<ForbiddenException> {
            taskService.update(1L, UpdateTaskRequest(title = "Hack"), otherEmail)
        }
        verify(taskRepository, never()).save(any())
    }

    // ── Mark Done ─────────────────────────────────────────────────────────────

    @Test
    fun `markDone - sets status to DONE`() {
        val user = mockUser()
        val task = mockTask(user)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))
        whenever(taskRepository.save(any())).thenReturn(task)

        val result = taskService.markDone(1L, ownerEmail)

        assertEquals(TaskStatus.DONE, task.status)
        verify(taskRepository).save(task)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete - removes task when owner requests it`() {
        val user = mockUser()
        val task = mockTask(user)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))

        taskService.delete(1L, ownerEmail)

        verify(taskRepository).delete(task)
    }

    @Test
    fun `delete - throws ForbiddenException when non-owner tries to delete`() {
        val owner = mockUser(ownerEmail)
        val task = mockTask(owner)

        whenever(taskRepository.findById(1L)).thenReturn(Optional.of(task))

        assertThrows<ForbiddenException> { taskService.delete(1L, otherEmail) }
        verify(taskRepository, never()).delete(any<Task>())
    }
}
