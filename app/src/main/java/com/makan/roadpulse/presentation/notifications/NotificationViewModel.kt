package com.makan.roadpulse.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.roadpulse.data.service.NotificationService
import com.makan.roadpulse.domain.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationService: NotificationService
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = notificationService.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markAsRead(id: String) {
        viewModelScope.launch { notificationService.markAsRead(id) }
    }

    fun markAllAsRead() {
        viewModelScope.launch { notificationService.markAllAsRead() }
    }
}
