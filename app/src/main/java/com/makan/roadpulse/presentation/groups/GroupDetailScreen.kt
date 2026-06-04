package com.makan.roadpulse.presentation.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.UserProfile
import com.makan.roadpulse.presentation.theme.CoralPrimary
import com.makan.roadpulse.presentation.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onOpenMap: (sessionId: String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val group            by viewModel.group.collectAsStateWithLifecycle()
    val memberProfiles   by viewModel.memberProfiles.collectAsStateWithLifecycle()
    val uninvitedFriends by viewModel.uninvitedFriends.collectAsStateWithLifecycle()
    val sessionStarted   by viewModel.sessionStarted.collectAsStateWithLifecycle()

    // Compute isOwner before the null-guard so dialogs can read it
    val isOwner = group?.ownerUid == viewModel.currentUid

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(sessionStarted) {
        sessionStarted?.let { sid ->
            onOpenMap(sid)
            viewModel.clearSessionStarted()
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(if (isOwner) "Disband group?" else "Leave group?") },
            text  = {
                Text(
                    if (isOwner)
                        "This will permanently delete the group and all session data."
                    else
                        "You will no longer be a member of this group."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isOwner) viewModel.disbandGroup() else viewModel.leaveGroup()
                    showLeaveDialog = false
                    onBack()
                }) {
                    Text(
                        if (isOwner) "Disband" else "Leave",
                        color = CoralPrimary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInviteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInviteSheet = false },
            sheetState = sheetState
        ) {
            InviteFriendSheet(
                friends  = uninvitedFriends,
                onInvite = { uid -> viewModel.inviteFriend(uid) }
            )
        }
    }

    val g = group
    if (g == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = TealPrimary) }
        return
    }

    val hasActiveSession = g.activeSessionId != null
    val isMember = viewModel.currentUid in g.memberUids
    val profileMap = memberProfiles.associateBy { it.uid }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(g.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (hasActiveSession) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Live", fontSize = 11.sp, color = Color.White,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Members card
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Members (${g.memberUids.size})", fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        g.memberUids.forEach { uid ->
                            val profile = profileMap[uid]
                            val name = profile?.displayName?.takeIf { it.isNotBlank() }
                                ?: "Loading…"
                            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            val roleLabel = if (uid == g.ownerUid) "Owner" else "Member"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatar(
                                    displayName = name,
                                    photoUrl    = profile?.photoUrl,
                                    initial     = initial,
                                    size        = 32
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(roleLabel, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Session controls — primary action first
            item {
                Spacer(Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (!hasActiveSession) {
                        Button(
                            onClick = { viewModel.startSession() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) { Text("Start group drive", fontWeight = FontWeight.Medium) }
                    } else if (isMember) {
                        Button(
                            onClick = { viewModel.joinSession() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) { Text("Join drive", fontWeight = FontWeight.Medium) }
                    }
                }
            }

            // Invite button (owner only)
            if (isOwner) {
                item {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showInviteSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Invite friend")
                    }
                }
            }

            // Leave / disband
            item {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPrimary)
                ) {
                    Text(if (isOwner) "Disband group" else "Leave group")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MemberAvatar(displayName: String, photoUrl: String?, initial: String, size: Int) {
    if (photoUrl != null) {
        AsyncImage(
            model              = photoUrl,
            contentDescription = null,
            modifier           = Modifier.size(size.dp).clip(CircleShape),
            contentScale       = ContentScale.Crop
        )
    } else {
        Box(
            modifier          = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(TealPrimary.copy(alpha = 0.7f)),
            contentAlignment  = Alignment.Center
        ) {
            Text(initial, fontSize = (size * 0.4f).sp,
                color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InviteFriendSheet(
    friends: List<UserProfile>,
    onInvite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Invite a friend", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        if (friends.isEmpty()) {
            Text("All friends are already in this group.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        } else {
            friends.forEach { friend ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInvite(friend.uid) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(
                        displayName = friend.displayName,
                        photoUrl    = friend.photoUrl,
                        initial     = friend.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        size        = 36
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(friend.displayName, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (friend.email.isNotBlank()) {
                            Text(friend.email, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
