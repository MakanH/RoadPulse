package com.makan.roadpulse.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.roadpulse.data.service.FriendService
import com.makan.roadpulse.data.service.GroupService
import com.makan.roadpulse.data.service.LocationTrackingService
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.UserProfile
import com.makan.roadpulse.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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

    private val friends: StateFlow<List<UserProfile>> = friendService.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Full UserProfile objects for every member UID in the group. */
    val memberProfiles: StateFlow<List<UserProfile>> = group
        .filterNotNull()
        .map { g -> friendService.getUserProfiles(g.memberUids) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Friends not yet in the group — stays reactive as the group's memberUids changes. */
    val uninvitedFriends: StateFlow<List<UserProfile>> = combine(group, friends) { g, fs ->
        val memberSet = g?.memberUids?.toSet() ?: emptySet()
        fs.filter { it.uid !in memberSet }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sessionStarted = MutableStateFlow<String?>(null)
    val sessionStarted: StateFlow<String?> = _sessionStarted.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent = _errorEvent.asSharedFlow()

    fun startSession() {
        val g = group.value ?: return
        viewModelScope.launch {
            groupService.startGroupSession(
                groupId         = groupId,
                groupName       = g.name,
                otherMemberUids = g.memberUids.filter { it != currentUid }
            ).onSuccess { sessionId ->
                LocationTrackingService.startGroupBroadcast(groupId, sessionId)
                _sessionStarted.value = sessionId
            }.onFailure {
                _errorEvent.tryEmit("Failed to start drive — please try again")
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
        viewModelScope.launch {
            groupService.inviteToGroup(groupId, friendUid).onFailure {
                _errorEvent.tryEmit("Failed to invite — please try again")
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            groupService.leaveGroup(groupId).onFailure {
                _errorEvent.tryEmit("Failed to leave group")
            }
        }
    }

    fun disbandGroup() {
        viewModelScope.launch {
            groupService.disbandGroup(groupId).onFailure {
                _errorEvent.tryEmit("Failed to disband group — please try again")
            }
        }
    }
}
