package com.makan.roadpulse.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.roadpulse.data.service.FriendService
import com.makan.roadpulse.domain.model.FriendRequest
import com.makan.roadpulse.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendService: FriendService
) : ViewModel() {

    data class FriendsUiState(
        val friends: List<UserProfile> = emptyList(),
        val incomingRequests: List<FriendRequest> = emptyList(),
        val outgoingRequests: List<FriendRequest> = emptyList()
    )

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent = _errorEvent.asSharedFlow()

    val uiState: StateFlow<FriendsUiState> = combine(
        friendService.getFriends(),
        friendService.getIncomingRequests(),
        friendService.getOutgoingRequests()
    ) { friends, incoming, outgoing ->
        FriendsUiState(friends, incoming, outgoing)
    }
        .catch { emit(FriendsUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    val searchQuery = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    // Optimistic pending set — updated immediately on tap to prevent double-sends
    private val _localPendingUids = MutableStateFlow<Set<String>>(emptySet())
    val localPendingUids: StateFlow<Set<String>> = _localPendingUids.asStateFlow()

    init {
        viewModelScope.launch {
            searchQuery.debounce(500).collect { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                } else {
                    try {
                        _searchResults.value = friendService.searchUsers(query)
                    } catch (e: Exception) {
                        _errorEvent.tryEmit("Search failed — check your connection")
                    }
                }
            }
        }
    }

    fun sendRequest(toUid: String) {
        if (toUid in _localPendingUids.value) return
        _localPendingUids.value = _localPendingUids.value + toUid
        viewModelScope.launch {
            friendService.sendFriendRequest(toUid).onFailure {
                _localPendingUids.value = _localPendingUids.value - toUid
                _errorEvent.tryEmit("Failed to send friend request")
            }
        }
    }

    fun acceptRequest(requestId: String, fromUid: String) {
        viewModelScope.launch {
            friendService.acceptRequest(requestId, fromUid).onFailure {
                _errorEvent.tryEmit("Failed to accept request")
            }
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            friendService.declineRequest(requestId).onFailure {
                _errorEvent.tryEmit("Failed to decline request")
            }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            friendService.removeFriend(friendUid).onFailure {
                _errorEvent.tryEmit("Failed to remove friend")
            }
        }
    }
}
