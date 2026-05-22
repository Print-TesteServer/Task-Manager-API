package com.delta.taskmanager.repository

import com.delta.taskmanager.entity.Task
import com.delta.taskmanager.entity.TaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findAllByUserId(userId: Long): List<Task>
    fun findAllByUserIdAndStatus(userId: Long, status: TaskStatus): List<Task>
}
