package com.exclr8.xen4.model

data class TaskListResponse(
    val Exception: Any,
    val Offline: Boolean,
    val Success: Boolean,
    val Tasks: List<Task>
)