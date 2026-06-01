package com.makan.roadpulse.domain.model

data class Group(
    val id: String,
    val name: String,
    val ownerUid: String,
    val memberUids: List<String>,
    val activeSessionId: String?
)
