package com.makan.roadpulse.domain.model

data class AppNotification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val actionData: Map<String, String> = emptyMap()
)
