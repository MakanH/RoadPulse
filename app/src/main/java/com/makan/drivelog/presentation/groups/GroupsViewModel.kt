package com.makan.drivelog.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.data.service.FriendService
import com.makan.drivelog.data.service.GroupService
import com.makan.drivelog.domain.model.Group
import com.makan.drivelog.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupService: GroupService,
    private val friendService: FriendService
) : ViewModel() {

    val groups: StateFlow<List<Group>> = groupService.getMyGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val friends: StateFlow<List<UserProfile>> = friendService.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _createResult = MutableStateFlow<Result<String>?>(null)
    val createResult: StateFlow<Result<String>?> = _createResult.asStateFlow()

    fun createGroup(name: String, friendUids: List<String>) {
        viewModelScope.launch {
            _createResult.value = groupService.createGroup(name, friendUids)
        }
    }

    fun clearCreateResult() { _createResult.value = null }
}
