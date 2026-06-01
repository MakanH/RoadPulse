package com.makan.drivelog.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.drivelog.data.service.FriendService
import com.makan.drivelog.data.service.GroupService
import com.makan.drivelog.data.service.LocationTrackingService
import com.makan.drivelog.domain.model.Group
import com.makan.drivelog.domain.model.UserProfile
import com.makan.drivelog.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupService: GroupService,
    private val friendService: FriendService,
    private val auth: FirebaseAuth
) : ViewModel() {

    val groupId: String = savedStateHandle.get<String>(Screen.GroupDetail.ARG_GROUP_ID) ?: ""
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    val group: StateFlow<Group?> = groupService.getGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val friends: StateFlow<List<UserProfile>> = friendService.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sessionStarted = MutableStateFlow<String?>(null)
    val sessionStarted: StateFlow<String?> = _sessionStarted.asStateFlow()

    fun startSession() {
        viewModelScope.launch {
            groupService.startGroupSession(groupId).onSuccess { sessionId ->
                LocationTrackingService.startGroupBroadcast(groupId, sessionId)
                _sessionStarted.value = sessionId
            }
        }
    }

    fun joinSession() {
        val sessionId = group.value?.activeSessionId ?: return
        LocationTrackingService.startGroupBroadcast(groupId, sessionId)
        _sessionStarted.value = sessionId
    }

    fun clearSessionStarted() { _sessionStarted.value = null }

    fun inviteFriend(friendUid: String) {
        viewModelScope.launch { groupService.inviteToGroup(groupId, friendUid) }
    }

    fun leaveGroup() {
        viewModelScope.launch { groupService.leaveGroup(groupId) }
    }
}
