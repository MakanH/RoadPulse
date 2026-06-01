package com.makan.roadpulse.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.roadpulse.data.service.GroupService
import com.makan.roadpulse.data.service.LocationTrackingService
import com.makan.roadpulse.domain.model.MemberLocation
import com.makan.roadpulse.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupService: GroupService,
    private val auth: FirebaseAuth
) : ViewModel() {

    val groupId: String  = savedStateHandle.get<String>(Screen.GroupMap.ARG_GROUP_ID) ?: ""
    val sessionId: String = savedStateHandle.get<String>(Screen.GroupMap.ARG_SESSION_ID) ?: ""
    val currentUid: String get() = auth.currentUser?.uid ?: ""

    val memberLocations: StateFlow<List<MemberLocation>> =
        groupService.getMemberLocations(groupId, sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val group = groupService.getGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun endSession() {
        viewModelScope.launch {
            groupService.endGroupSession(groupId, sessionId)
            LocationTrackingService.stopGroupBroadcast()
        }
    }

    fun stopSharing() {
        viewModelScope.launch {
            groupService.setMemberInactive(groupId, sessionId)
            LocationTrackingService.stopGroupBroadcast()
        }
    }
}
